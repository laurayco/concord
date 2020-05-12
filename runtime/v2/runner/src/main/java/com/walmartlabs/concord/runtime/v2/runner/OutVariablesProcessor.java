package com.walmartlabs.concord.runtime.v2.runner;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.v2.model.ProcessConfiguration;
import com.walmartlabs.concord.runtime.v2.runner.el.DefaultEvalContext;
import com.walmartlabs.concord.runtime.v2.runner.el.EvalContext;
import com.walmartlabs.concord.runtime.v2.runner.el.EvalContextFactory;
import com.walmartlabs.concord.runtime.v2.runner.el.ExpressionEvaluator;
import com.walmartlabs.concord.runtime.v2.sdk.GlobalVariables;
import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.svm.ExecutionListener;
import com.walmartlabs.concord.svm.Runtime;
import com.walmartlabs.concord.svm.State;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves the process' {@code out} variables into a persistent file.
 * Normally, the server picks up the file and saves the data into the process' metadata.
 */
public class OutVariablesProcessor implements ExecutionListener {

    private final ObjectMapper objectMapper;
    private final PersistenceService persistenceService;
    private final List<String> outVariables;

    @Inject
    public OutVariablesProcessor(PersistenceService persistenceService, ProcessConfiguration processConfiguration) {
        this.persistenceService = persistenceService;
        this.objectMapper = new ObjectMapper();
        this.outVariables = processConfiguration.out();
    }

    @Override
    public void afterProcessEnd(Runtime runtime, State state) {
        if (outVariables.isEmpty()) {
            return;
        }

        GlobalVariables globalVariables = runtime.getService(GlobalVariables.class);
        ExpressionEvaluator ee = runtime.getService(ExpressionEvaluator.class);
        EvalContext evalContext = DefaultEvalContext.builder().from(EvalContextFactory.strict(globalVariables.toMap()))
                .undefinedVariableAsNull(true)
                .build();

        Map<String, Object> outValues = new HashMap<>();
        for (String out : outVariables) {
            Object v = ee.eval(evalContext, "${" + out + "}", Object.class);

            if (v == null) {
                continue;
            }

            outValues.put(out, v);
        }

        if (outValues.isEmpty()) {
            return;
        }

        persistenceService.persistFile(Constants.Files.OUT_VALUES_FILE_NAME,
                out -> objectMapper.writeValue(out, outValues));
    }
}