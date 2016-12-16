package cn.jesse.patcher.build.gradle.task

import cn.jesse.patcher.build.gradle.PatcherPlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Created by jesse on 15/12/2016.
 */

public class PatchSchemaTask extends DefaultTask {
    def configuration
    def android
    String buildApkPath
    String outputFolder
    def signConfig

    PatchSchemaTask() {
        super()
        description = 'Assemble Patcher Patch'
        group = PatcherPlugin.PATCHER_PLUGIN_GROUP
        outputs.upToDateWhen { false }
        configuration = project.patcher

        android = project.extensions.android
    }
    @TaskAction
    def patch() {
        println('exec patch task')
    }
}
