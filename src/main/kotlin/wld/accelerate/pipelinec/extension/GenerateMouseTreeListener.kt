package wld.accelerate.pipelinec.extension

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.treeStructure.Tree
import wld.accelerate.pipelinec.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

fun generatingMouseTreeListener(generateTree: Tree, toolWindow: ToolWindow): MouseAdapter {
    return object : MouseAdapter() {
        override fun mouseClicked(mouseEvent: MouseEvent) {
            if (mouseEvent.clickCount === 2) { // Double-click
                val path: TreePath = generateTree.getPathForLocation(mouseEvent.x, mouseEvent.y)
                if (path != null) {
                    val lastPathComponent: Any = path.lastPathComponent
                    if (lastPathComponent is DefaultMutableTreeNode) {
                        val userObject = lastPathComponent.userObject

                        //val file = File("src/main/resources/" + classLoader.getResource(resourceName)!!.file)
                        if ("generate" == userObject) {

                        } else {

                            FileChooser.chooseFile(
                                FileChooserDescriptorFactory.createSingleFileDescriptor(),
                                toolWindow.project,
                                null
                            ) {

                                val file = File(it.path)
                                val yamlMap = parseYaml(file.absolutePath)
                                val generatingFunction =
                                    { generateSuffix: String, entityClassRepresentations: Map<String, String>, fileEnding: String ->
                                        val generatePath = toolWindow.project.basePath + generateSuffix
                                        Files.createDirectories(Path.of(generatePath))

                                        entityClassRepresentations.forEach { fileToContent ->
                                            File(generatePath + "/" + fileToContent.key + fileEnding).writeText(
                                                fileToContent.value
                                            )
                                        }
                                    }

                                if ("sql" == userObject) {
                                    generatingFunction(
                                        "/src/res/sql",
                                        writeDDL(yamlMap?.get("entities") as Map<String, Map<String, Any>>),
                                        ".sql"
                                    )
                                } else if ("entities" == userObject) {
                                    generatingFunction(
                                        "/src/res/entities", writeEntityDataClassJava(
                                            yamlMap?.get("entities") as Map<String, Map<String, Any>>,
                                            (yamlMap.get("packagePath") as String) + ".entities"
                                        ), ".java"
                                    )
                                } else if ("controllers" == userObject) {
                                    generatingFunction(
                                        "/src/res/controllers", writeJavaControllerClasses(
                                            (yamlMap?.get("entities") as Map<String, *>).keys.toList(),
                                            (yamlMap["packagePath"] as String) + ".java.controller"
                                        ), ".java"
                                    )

                                } else if ("vue-create" == userObject) {
                                    generatingFunction(
                                        "/src/res/vue/components",
                                        writeVueCreateForm(yamlMap?.get("entities") as Map<String, Map<String, Any>>),
                                        "Create.vue"
                                    )

                                } else if ("vue-details" == userObject) {
                                    generatingFunction(
                                        "/src/res/vue/components", writeVueDetailsComponentTemplate(
                                            yamlMap?.get("entities") as Map<String, Map<String, Any>>
                                        ), "Details.vue"
                                    )

                                } else if ("vue-list" == userObject) {
                                    generatingFunction(
                                        "/src/res/vue/components", writeVueListComponentTemplate(
                                            yamlMap?.get("entities") as Map<String, Map<String, Any>>
                                        ), "List.vue"
                                    )

                                } else if ("vue-landing-page" == userObject) {
                                    generatingFunction(
                                        "/src/res/vue/components",
                                        mapOf("LandingPage" to writeVueLandingPageComponentTemplate((yamlMap?.get("entities") as Map<String, *>).keys.toList())),
                                        ".vue"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun createMouseTreeListener(generateTree: Tree, toolWindow: ToolWindow): MouseAdapter {
    return object : MouseAdapter() {
        override fun mouseClicked(mouseEvent: MouseEvent) {
            if (mouseEvent.clickCount === 2) { // Double-click
                val path: TreePath = generateTree.getPathForLocation(mouseEvent.x, mouseEvent.y)
                if (path != null) {
                    val lastPathComponent: Any = path.lastPathComponent
                    if (lastPathComponent is DefaultMutableTreeNode) {
                        val userObject = lastPathComponent.userObject

                        Messages.showInfoMessage("" + mouseEvent.x + " - " + mouseEvent.y, "Tree Click")

                        val createConfigDialog = CreateConfigDialog()

                        if (createConfigDialog.showAndGet()) {
                            val entityName = createConfigDialog.fieldName?.text
                            val fields = mutableMapOf<String,Any>()

                            for(i in 0..(createConfigDialog.jbTable!!.model!!.rowCount - 1)){
                                fields[createConfigDialog.jbTable?.model?.getValueAt(i,0).toString()] = createConfigDialog.jbTable!!.model!!.getValueAt(i,1).toString()
                            }

                            //File(toolWindow.project.basePath + "/src/res/config.yaml")
                            //    .writeText(entityName + "\n" + fields.entries.joinToString(separator = "\n") { it.key + ": " + it.value })


                            val file = File(toolWindow.project.basePath + "/src/res/config.yaml")

                            try { parseYaml(file.absolutePath) }
                            catch (e: FileNotFoundException) {
                                File(toolWindow.project.basePath + "/src/res/config.yaml").writeText("")
                            }
                            val yamlMap = parseYaml(file.absolutePath)



                            val entitiesMap: MutableMap<String, Map<String, Any>> =
                                if(yamlMap?.containsKey("entities") == true) yamlMap["entities"] as MutableMap<String, Map<String, Any>>
                                else mutableMapOf<String, Map<String, Any>>()
                            entitiesMap[entityName!!] = fields as Map<String, Any>

                            file.writeText("\n" + "entities:" + "\n  " + entityName + ":\n        " + fields.entries.joinToString(separator = "\n        ") { it.key + ": " + it.value })
                            Messages.showInfoMessage( "\n" + "entities:" + "\n\t" + fields.entries.joinToString(separator = "\n\t\t") { it.key + ": " + it.value },"entity")
                        }
                    }
                }
            }
        }
    }
}