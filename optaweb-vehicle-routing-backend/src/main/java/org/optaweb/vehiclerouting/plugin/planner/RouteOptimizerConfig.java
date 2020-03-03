/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaweb.vehiclerouting.plugin.planner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaweb.vehiclerouting.plugin.planner.domain.VehicleRoutingSolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Spring configuration that creates {@link RouteOptimizerImpl route optimizer}'s dependencies.
 */
@Slf4j
@Configuration
class RouteOptimizerConfig {
    
    @Value("${app.solver.config-dir:solver}")
    private String solverConfigDir;
    
    @Value("${app.solver.config-xml:vehicleRoutingSolverConfig.xml}")
    private String solverConfigXml;


    static final String DEFAULT_SOLVER_CONFIG = "org/optaweb/vehiclerouting/solver/vehicleRoutingSolverConfig.xml";

    private final OptimizerProperties optimizerProperties;

    @Autowired
    RouteOptimizerConfig(OptimizerProperties optimizerProperties) {
        this.optimizerProperties = optimizerProperties;
    }

    @Bean
    Solver<VehicleRoutingSolution> solver() {
        SolverConfig solverConfig = getSolverConfig();
        Duration timeout = optimizerProperties.getTimeout();
        solverConfig.setTerminationConfig(new TerminationConfig().withSecondsSpentLimit(timeout.getSeconds()));
        solverConfig.setDaemon(true);
        return SolverFactory.<VehicleRoutingSolution>create(solverConfig).buildSolver();
    }

    @Bean
    AsyncTaskExecutor executor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setConcurrencyLimit(1);
        return executor;
    }
    
    
    /*
    Dynamic solver config.
    */
    private SolverConfig getSolverConfig() {
        /*
        Load based on precedent
        1. from ./solver folder
        */
        
        // User provided file exists?
        Path path = Paths.get(solverConfigDir);
        boolean configExists = Files.exists(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
        if (configExists) {
            log.info("Loading SolverConfig XML file from : {}", path);
            
            // Load solver directory to classpath
            URLClassLoader sysClassLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
            Class sysclass = URLClassLoader.class;
            final Class[] parameters = new Class[]{URL.class};
            
            try {
                Method method = sysclass.getDeclaredMethod("addURL", parameters);
                method.setAccessible(true);
                method.invoke(sysClassLoader, new Object[]{path.toFile().toURI().toURL()});
            } catch (MalformedURLException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                log.error("Error loading Optimizer rule.", ex);
            }
            
            return SolverConfig.createFromXmlFile(Paths.get(path.toString(), solverConfigXml).toFile());
        }

        // Use context classloader to avoid ClassCastException during solution cloning:
        // https://stackoverflow.com/questions/52586747/classcastexception-occured-on-solver-solve
        // as recommended in
        // CHECKSTYLE:OFF
        // https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-devtools.html#using-boot-devtools-customizing-classload
        // CHECKSTYLE:ON
        log.info("No user defined SolverConfig XML file exist at {}. Using Default", solverConfigDir);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return SolverConfig.createFromXmlResource(DEFAULT_SOLVER_CONFIG, classLoader);
    }
}
