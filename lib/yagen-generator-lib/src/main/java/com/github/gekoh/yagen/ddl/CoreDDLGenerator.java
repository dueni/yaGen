/*
 Copyright 2014 Georg Kohlweiss

 Licensed under the Apache License, Version 2.0 (the License);
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an AS IS BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package com.github.gekoh.yagen.ddl;

import com.github.gekoh.yagen.hibernate.YagenInit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author Georg Kohlweiss
 */
public class CoreDDLGenerator {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CoreDDLGenerator.class);

    private static final String PARAM_PROFILE_NAME = "profile-name";
    public static final String PARAM_PROFILE_PROVIDER_CLASS = "profile-provider-class";
    private static final String PARAM_PERSISTENCE_UNIT_NAME = "persistence-unit-name";
    private static final String PARAM_OUTPUT_FILENAME = "output-file";
    private static final String PARAM_HEADER_DDLS_LIST = "header-ddl-list";
    private static final String PARAM_ADDITIONAL_DDLS_LIST = "additional-ddl-list";
    private static final String PARAM_DISABLE_FKS = "disable-foreign-keys";
    private static final String PARAM_REGEX_RENDER_ONLY_ENTITIES = "only-entities-regex";
    private static final String PARAM_NO_HISTORY_GENERATION = "no-history";

    public static final Options OPTIONS = new Options();
    static {
        addOption(PARAM_PROFILE_NAME, true, "name of profile to be used");
        addOption(PARAM_OUTPUT_FILENAME, true, "path of generated ddl file");
        addOption(PARAM_PROFILE_PROVIDER_CLASS, true, "full qualified class name of profile provider");
        addOption(PARAM_PERSISTENCE_UNIT_NAME, true, "name of the persistent unit holding the entity configuration");
        addOption(PARAM_HEADER_DDLS_LIST, true, "semicolon separated list of header include files");
        addOption(PARAM_ADDITIONAL_DDLS_LIST, true, "semicolon separated list of footer include files");
        addOption(PARAM_REGEX_RENDER_ONLY_ENTITIES, true, "entities matching specified regex will be rendered");
        addOption(PARAM_DISABLE_FKS, false, "indicates that foreign keys should be initially disabled");
        addOption(PARAM_NO_HISTORY_GENERATION, false, "indicates that no history tables should be generated even if @TemporalEntity is used");
    }
    private static void addOption(String longOpt, boolean hasArg, String description) {
        OPTIONS.addOption(null, longOpt, hasArg, description);
    }

    public static void main(String[] args) {
        try {
            generateFrom(createProfileFrom(args));
        } catch (ParseException e) {
            LOG.error("error parsing arguments", e);
        }
    }

    public static void generateFrom(DDLGenerator.Profile profile) {
        try {
            YagenInit.init(profile);
        } catch (Exception e) {
            throw new IllegalStateException("cannot init patches for ddl generator", e);
        }

        profile.addHeaderDdlOnTop(new DDLGenerator.AddTemplateDDLEntry(
                "#if( ${dialect.getClass().getSimpleName().toLowerCase().contains('oracle')} )\n" +
                "-- this prevents us from being asked by the executing SQL console to replace a variable\n" +
                "-- when using entity declarations like &amp; in varchar values\n" +
                "-- this works in sqlplus, SqlDeveloper and TOAD (execute as script)\n" +
                "set define off;\n" +
                "-- this stops execution on first error to sort out the problem and then continue manually\n" +
                "WHENEVER SQLERROR EXIT FAILURE;\n" +
                "#end"));

/*
        try {
            PROFILE.addDuplexer(new com.github.gekoh.yagen.ddl.AuditTriggerDuplexer(new java.io.File("auditTriggers_"+ PROFILE.getName()+".ddl.sql"), "\n/\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
*/

        new DDLGenerator().writeDDL(profile);
    }

    public static DDLGenerator.Profile createProfileFrom(String[] args) throws ParseException {
        DDLGenerator.Profile profile = null;
        CommandLineParser clp = new GnuParser();
        CommandLine cl = clp.parse(OPTIONS, args);

        try {

            if (cl.hasOption(PARAM_PROFILE_PROVIDER_CLASS)) {
                profile = ((ProfileProvider) Class.forName(cl.getOptionValue(PARAM_PROFILE_PROVIDER_CLASS)).newInstance())
                        .getProfile(cl.getOptionValue(PARAM_PROFILE_NAME));
            }
            else {
                profile = new DDLGenerator.Profile(cl.getOptionValue(PARAM_PROFILE_NAME));
            }

            if (cl.hasOption(PARAM_OUTPUT_FILENAME)) {
                profile.setOutputFile(cl.getOptionValue(PARAM_OUTPUT_FILENAME));
            }

            if (cl.hasOption(PARAM_PERSISTENCE_UNIT_NAME)) {
                profile.setPersistenceUnitName(cl.getOptionValue(PARAM_PERSISTENCE_UNIT_NAME));
            }

            if (cl.hasOption(PARAM_HEADER_DDLS_LIST)) {
                profile.addHeaderDdlFile(cl.getOptionValue(PARAM_HEADER_DDLS_LIST).split(";[\\s]*"));
            }

            if (cl.hasOption(PARAM_ADDITIONAL_DDLS_LIST)) {
                profile.addDdlFile(cl.getOptionValue(PARAM_ADDITIONAL_DDLS_LIST).split(";[\\s]*"));
            }

            if (cl.hasOption(PARAM_DISABLE_FKS)) {
                profile.setDisableFKs(true);
            }

            if (cl.hasOption(PARAM_NO_HISTORY_GENERATION)) {
                profile.setNoHistory(true);
            }

            if (cl.hasOption(PARAM_REGEX_RENDER_ONLY_ENTITIES)) {
                profile.setOnlyRenderEntitiesRegex(cl.getOptionValue(PARAM_REGEX_RENDER_ONLY_ENTITIES));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("cannot instantiate profile provider class " + cl.getOptionValue(PARAM_PROFILE_PROVIDER_CLASS));
        } catch (Exception e) {
            throw new IllegalStateException("error setting up generator profile: " + e.getMessage(), e);
        }

        return profile;
    }
}
