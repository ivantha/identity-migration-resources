/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.is.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.config.Config;
import org.wso2.carbon.is.migration.config.MigratorConfig;
import org.wso2.carbon.is.migration.config.Version;
import org.wso2.carbon.is.migration.service.ClaimDataMigrator;
import org.wso2.carbon.is.migration.service.EmailTemplateDataMigrator;
import org.wso2.carbon.is.migration.service.Migrator;
import org.wso2.carbon.is.migration.service.SchemaMigrator;
import org.wso2.carbon.is.migration.util.Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for version migration.
 */
public abstract class VersionMigration {

    private static final Logger log = LoggerFactory.getLogger(VersionMigration.class);

    public void migrate() throws MigrationClientException {

        String dryRun = System.getProperty(Constant.DRY_RUN);

        List<Migrator> migrators = getMigrators();
        for (Migrator migrator : migrators) {
            log.info(Constant.MIGRATION_LOG + "Version : " + getCurrentVersion() + ", Migration Step : " +
                    migrator.getClass().getSimpleName() + " of order : " + migrator.getMigratorConfig().getOrder() +
                    " is starting........................... ");
            if (dryRun != null) {
                migrator.dryRun();
            } else {
                migrator.migrate();
            }
        }
    }

    public List<Migrator> getMigrators() throws MigrationClientException {

        List<Migrator> migrators = new ArrayList<>();
        Version version = getMigrationConfig();
        List<MigratorConfig> migratorConfigs = version.getMigratorConfigs();
        for (MigratorConfig migratorConfig : migratorConfigs) {
            String migratorName = migratorConfig.getName();
            Migrator migrator = null;
            if (migratorName.equals(Constant.SCHEMA_MIGRATOR_NAME)) {
                migrator = new SchemaMigrator();
            } else {
                migrator = getMigrator(migratorName);
            }
            migrator.setMigratorConfig(migratorConfig);
            migrator.setVersionConfig(version);
            migrators.add(migrator);
        }
        return migrators;
    }

    public abstract String getPreviousVersion();

    public abstract String getCurrentVersion();

    public Migrator getMigrator(String migratorName) {

        Package aPackage = this.getClass().getPackage();
        String basePackage = aPackage.getName() + ".migrator";
        try {
            Class<?> migratorClass = Class.forName(basePackage + "." + migratorName);
            return (Migrator) migratorClass.newInstance();
        } catch (ClassNotFoundException e) {
            if (migratorName.equals(Constant.CLAIM_DATA_MIGRATOR_NAME)) {
                return new ClaimDataMigrator();
            }
            if (migratorName.equals(Constant.EMAIL_TEMPLATE_DATA_MIGRATOR_NAME)) {
                return new EmailTemplateDataMigrator();
            }
            log.error("Migrator class not exists for: " + migratorName);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Error while creating migration instance", e);
        }
        return null;
    }

    public Version getMigrationConfig() throws MigrationClientException {

        Config config = Config.getInstance();
        Version version = config.getMigrateVersion(getCurrentVersion());
        return version;
    }

}
