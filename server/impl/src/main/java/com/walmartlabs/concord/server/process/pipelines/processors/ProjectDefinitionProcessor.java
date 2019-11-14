package com.walmartlabs.concord.server.process.pipelines.processors;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.walmartlabs.concord.project.ProjectLoader;
import com.walmartlabs.concord.project.model.ProjectDefinition;
import com.walmartlabs.concord.repository.Snapshot;
import com.walmartlabs.concord.server.process.ImportsNormalizerFactory;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.process.ProcessException;
import com.walmartlabs.concord.server.process.ProcessKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.*;

@Named
@Singleton
public class ProjectDefinitionProcessor implements PayloadProcessor {

    private static final Logger log = LoggerFactory.getLogger(ProjectDefinitionProcessor.class);

    private final ProjectLoader projectLoader;
    private final ImportsNormalizerFactory importsNormalizer;

    @Inject
    public ProjectDefinitionProcessor(ProjectLoader projectLoader,
                                      ImportsNormalizerFactory importsNormalizer) {

        this.projectLoader = projectLoader;
        this.importsNormalizer = importsNormalizer;
    }

    @Override
    public Payload process(Chain chain, Payload payload) {
        ProcessKey processKey = payload.getProcessKey();

        Path workDir = payload.getHeader(Payload.WORKSPACE_DIR);
        if (workDir == null) {
            return chain.process(payload);
        }

        // configuration from the user request
        Map<String, Object> cfg = payload.getHeader(Payload.CONFIGURATION, Collections.emptyMap());

        try {
            UUID projectId = payload.getHeader(Payload.PROJECT_ID);
            ProjectLoader.Result result = projectLoader.loadProject(workDir, importsNormalizer.forProject(projectId), cfg);

            List<Snapshot> snapshots = result.getSnapshots();
            for (Snapshot s : snapshots) {
                payload = addSnapshot(payload, s);
            }

            ProjectDefinition pd = result.getProjectDefinition();
            payload = payload.putHeader(Payload.PROJECT_DEFINITION, pd);

            payload = payload.putHeader(Payload.IMPORTS, pd.getImports());

            return chain.process(payload);
        } catch (Exception e) {
            log.warn("process -> ({}) project loading error: {}", workDir, e.getMessage());
            throw new ProcessException(processKey, "Error while loading the project, check the syntax. " + e.getMessage(), e);
        }
    }

    private static Payload addSnapshot(Payload payload, Snapshot s) {
        List<Snapshot> result = new ArrayList<>();

        List<Snapshot> snapshots = payload.getHeader(RepositoryProcessor.REPOSITORY_SNAPSHOT);
        if (snapshots != null) {
            result.addAll(snapshots);
        }
        result.add(s);

        return payload.putHeader(RepositoryProcessor.REPOSITORY_SNAPSHOT, result);
    }
}
