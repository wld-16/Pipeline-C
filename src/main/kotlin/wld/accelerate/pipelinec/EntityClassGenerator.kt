package wld.accelerate.pipelinec

import wld.accelerate.pipelinec.extension.capitalize
import wld.accelerate.pipelinec.extension.unCapitalize

fun writeVueTemplate(entities: Map<String, Map<String, Any>>): Map<String, String> {
    val scriptsTagHead: (String) -> String =
        { "<script setup>\nimport { reactive } from 'vue'\nconst $it = reactive({\n" }
    val scriptsTagFields: (String) -> String = {
        "  $it: undefined"
    }
    val scriptsTagFoot = "\n\t})\n</script>"

    val scriptTags = entities.entries.associate {
        val fields = it.value.keys.joinToString(separator = ",\n") { field -> scriptsTagFields(field) }
        it.key to (scriptsTagHead(it.key) + fields + scriptsTagFoot + "\n" + "\n")
    }

    val templateTags = entities.keys.associateWith {
        "<template>\n" +
                "  <v-table>\n" +
                "    <tbody>\n" +
                "    <tr v-for=\"(value, key) in ${it}\" class=\"text-left\">\n" +
                "      <td>{{ key }}</td>\n" +
                "      <td>{{ value }}</td>\n" +
                "    </tr>\n" +
                "    </tbody>\n" +
                "  </v-table>\n" +
                "</template>"
    }

    return entities.keys.associateWith { scriptTags[it] + templateTags[it] }
}

fun writeDDL(entities: Map<String, Map<String, Any>>): Map<String, String> {
    val sqlTableHead: (String) -> String = { "CREATE TABLE $it (\n" }
    val sqlTableFieldTemplate: (String, String) -> String = { fieldName, fieldType ->
        "\t$fieldName: " +
                if (fieldType == "Integer") {
                    "INTEGER,"
                } else if (fieldType == "Enum") {
                    "VARCHAR,"
                } else {
                    "VARCHAR,"
                }
    }
    val sqlTableFoot = "\n);"

    val fieldTemplate: (String, String) -> String = { fieldName: String, fieldType: String ->
        "$fieldName: $fieldType" +
                when (fieldType) {
                    "String" -> "\"\""
                    else -> "null"
                }
    }

    val sqlTableField = entities.entries.associate { entity ->
        val fields = (entity.value as Map<String, *>).entries.joinToString(separator = "\n") {
            sqlTableFieldTemplate(it.key, it.value as String)
        }
        val returnValue = sqlTableHead(entity.key) +
                fields.substring(0, fields.length - 1) +
                sqlTableFoot
        entity.key to returnValue
    }
    return sqlTableField
}

fun writeJavaEnums(enums: LinkedHashMap<String, String>, packagePath: String): Map<String, String> {
    val head: (String) -> String = { "package $packagePath.java;\n\npublic enum $it {" }
    val entry: (String) -> String = { "\n\t$it," }
    val foot: String = "\n}"

    val enumsFileMap = enums.entries.associate {
        it.key to
                head(it.key) + (it.value.filterNot { it == '[' || it == ']' || it == ' ' }.split(",").map { entry(it) }
            .joinToString(separator = "")) + foot
    }

    return enumsFileMap
}

fun writeKotlinEnums(enums: LinkedHashMap<String, String>, packagePath: String): Map<String, String> {
    val head: (String) -> String = { "package $packagePath.kotlin\n\nenum class $it {" }
    val entry: (String) -> String = { "\n\t$it," }
    val foot: String = "\n}"

    val enumsFileMap = enums.entries.associate {
        it.key to
                head(it.key) + (it.value.filterNot { it == '[' || it == ']' || it == ' ' }.split(",").map { entry(it) }
            .joinToString(separator = "")) + foot
    }

    return enumsFileMap
}

fun writeJavaControllerClasses(controllerEntities: List<String>, packagePath: String): Map<String, String> {
    val packageStatement: String = "package $packagePath;\n"

    val importStatements: (String) -> String = {
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.http.ResponseEntity;\n" +
                "import org.springframework.web.bind.annotation.GetMapping;\n" +
                "import org.springframework.web.bind.annotation.PostMapping;\n" +
                "import org.springframework.web.bind.annotation.PathVariable;\n" +
                "import org.springframework.web.bind.annotation.RestController;\n" +
                "import wld.accelerate.pipelinec.java.entity.${it};\n" +
                "import wld.accelerate.pipelinec.java.model.${it}Model;\n" +
                "import wld.accelerate.pipelinec.java.service.${it}Service;\n" +
                "import java.util.List;"
    }

    val classStatement: (String) -> String = {
        "\n@RestController\n" +
                "public class ${it}Controller {"
    }

    val classClose: String = "}"

    val autoWiredComponentsStatement: (String) -> String = {
        "\n\t@Autowired\n" +
                "\tprivate ${it}Service ${String.unCapitalize(it)}Service;\n"
    }

    val getByIdEndpointTemplate: (String) -> String = {
        "\t@GetMapping(\"/${String.unCapitalize(it)}/{id}\")\n" +
                "\tpublic ResponseEntity<${it}Model> get${it}(@PathVariable Integer id) {\n" +
                "\t\treturn ResponseEntity.ok(${it}Model.from${it}(${String.unCapitalize(it)}Service.findById(id)));\n" +
                "\t}"
    }

    val getAllEndpointTemplate: (String) -> String = {
        "\t@GetMapping(\"/${String.unCapitalize(it)}/\")\n" +
                "\tpublic ResponseEntity<List<${it}Model>> getAll${it}() {\n" +
                "\t\treturn ResponseEntity.ok(${String.unCapitalize(it)}Service.findAll().stream().map(${it}Model::from${it}).toList());\n" +
                "\t}"
    }

    val saveEndpointTemplate: (String) -> String = {
        "\t@PostMapping(\"/${String.unCapitalize(it)}/\")\n" +
                "\tpublic ResponseEntity<${it}Model> save${it}(${it}Model ${String.unCapitalize(it)}Model) {\n" +
                "\t\t${it} ${String.unCapitalize(it)} = ${String.unCapitalize(it)}Service.create${it}(${String.unCapitalize(it)}Model);\n" +
                "\t\treturn ResponseEntity.ok(${it}Model.from${it}(${String.unCapitalize(it)}));\n" +
                "\t}"
    }

    val updateEndpointTemplate: (String) -> String = {
        "\t@PostMapping(\"/activity/{id}\")\n" +
                "\tpublic ResponseEntity<ActivityModel> updateActivity(@PathVariable Integer id, ActivityModel activityModel) {\n" +
                "\t\tActivity activity = activityService.updateActivity(id, activityModel);\n" +
                "\t\treturn ResponseEntity.ok(ActivityModel.fromActivity(activity));\n" +
                "\t}"
    }

    return controllerEntities.associateWith {
                packageStatement + "\n" +
                importStatements(it) + "\n" +
                classStatement(it) + "\n" +
                autoWiredComponentsStatement(it) + "\n" +
                getByIdEndpointTemplate(it) + "\n" +
                getAllEndpointTemplate(it) + "\n" +
                saveEndpointTemplate(it) + "\n" +
                updateEndpointTemplate(it) + "\n" +
                classClose
    }
}

fun writeKotlinControllerClasses(controllerEntities: List<String>, packagePath: String): Map<String, String> {
    val importPath: (String) -> String = {
        "import org.springframework.data.jpa.repository.JpaRepository\n" +
                "import org.springframework.stereotype.Repository\n" +
                "import wld.accelerate.pipelinec.$it"
    }

    val interfaceString: (String) -> String = {
        "\n@Repository\n" +
                "interface ${it}Repository: JpaRepository<${it}, Int> {\n" +
                "}"
    }
    return controllerEntities.associateWith {
        "package " + packagePath + "\n\n" + importPath(it) + "\n\n" + interfaceString(
            it
        )
    }
}

fun writeJavaRepositoryDataClass(entities: List<String>, packagePath: String): Map<String, String> {
    val importPath: (String) -> String = {
        "import org.springframework.data.jpa.repository.JpaRepository;\n" +
                "import org.springframework.stereotype.Repository;\n" +
                "import wld.accelerate.pipelinec.java.entity.$it;"
    }

    val interfaceString: (String) -> String = {
        "\n@Repository\n" +
                "public interface ${it}Repository extends JpaRepository<${it}, Integer> {\n" +
                "}"
    }
    return entities.associateWith { "package " + packagePath + ";" + "\n\n" + importPath(it) + "\n\n" + interfaceString(it) }
}

fun writeKotlinRepositoryDataClass(entities: List<String>, packagePath: String): Map<String, String> {
    val importPath: (String) -> String = {
        "import org.springframework.data.jpa.repository.JpaRepository\n" +
                "import org.springframework.stereotype.Repository\n" +
                "import wld.accelerate.pipelinec.kotlin.entity.$it"
    }

    val interfaceString: (String) -> String = {
        "\n@Repository\n" +
                "interface ${it}Repository: JpaRepository<${it}, Int> {\n" +
                "}"
    }
    return entities.associateWith { "package " + packagePath + "\n\n" + importPath(it) + "\n\n" + interfaceString(it) }
}

fun writeJavaModelDataClass(models: Map<String, Map<String, Any>>, packagePath: String): Map<String, String> {
    val enumImportTemplate: (String) -> String =
        { if (it == "String" || it == "Integer") "" else "\nimport $packagePath.java.$it;" }
    val defaultIdTemplate: String = "\tprivate Long id;\n"

    val importStatements: (String) -> (String) = { "\nimport $packagePath.java.entity.$it;" }

    val fieldTemplate: (String, String) -> String = { fieldName: String, fieldType: String ->
        "\tprivate $fieldType $fieldName;"
    }

    val getMethodTemplate: (String, String) -> String = { fieldName, fieldType ->
        "\n\tpublic $fieldType get${String.capitalize(fieldName)}(){\n\t\treturn $fieldName;\n\t}\n"
    }

    val setMethodTemplate: (String, String) -> String = { fieldName, fieldType ->
        "\n\tpublic void set${String.capitalize(fieldName)}($fieldType $fieldName) {" +
                "\n\t\tthis.$fieldName = $fieldName;\n\t}\n"
    }

    val fromTemplate: (String, Map<String, String>) -> String = { it, map ->
        "\tpublic static ${it}Model from$it($it ${String.unCapitalize(it)}) {\n" +
                "\t\t${it}Model ${it.lowercase()}Model = new ${it}Model();\n" +
                map.entries.joinToString(separator = "") { entry -> "\t\t${it.lowercase()}Model." + entry.key + " = " + entry.value + ";\n" } +
                "\t\treturn ${it.lowercase()}Model;\n" +
                "\t}"
    }

    val toModelTemplate: (String, Map<String, String>) -> String = { it, map ->
        "\tpublic static $it to$it(${it}Model ${String.unCapitalize(it)}Model){\n" +
                "\t\t$it ${String.unCapitalize(it)} = new $it();\n" +
                map.entries.joinToString(separator = "") {
                    entry -> "\t\t${it.lowercase()}Model.set${String.capitalize(entry.key)}(${entry.value});\n" } +
                "\t\treturn ${it.lowercase()};\n" +
                "\t}"

    }

    val dataPackageHead: (String) -> String = { "package $it.java.model;\n" }

    val dataClassHead: (String) -> String = { "\n\npublic class ${it}Model {\n" }
    val dataClassFoot = "\n}"

    val classesAsStrings = models
        .entries.associate { entity ->
            val returnValue = dataPackageHead(packagePath) +
                    importStatements(entity.key) +
                    (entity.value as Map<String, *>).entries.joinToString(separator = "") {
                        enumImportTemplate(it.value as String)
                    } +
                    dataClassHead(entity.key) +
                    defaultIdTemplate +
                    (entity.value as Map<String, *>).entries.joinToString(separator = "\n") {
                        fieldTemplate(it.key, it.value as String)
                    } + "\n" +
                    fromTemplate(entity.key, entity.value.entries.associate { entry -> entry.key to "${String.unCapitalize(entity.key)}.get${String.capitalize(entry.key)}()" }) + "\n" +
                    toModelTemplate(entity.key, entity.value.entries.associate { entry -> entry.key to "${String.unCapitalize(entity.key)}.get${String.capitalize(entry.key)}()" }) + "\n" +
                    (entity.value as Map<String, *>).entries.joinToString(separator = "") {
                        getMethodTemplate(it.key, it.value as String)
                    } +
                    (entity.value as Map<String, *>).entries.joinToString(separator = "") {
                        setMethodTemplate(it.key, it.value as String)
                    } + dataClassFoot
            entity.key to returnValue
        }

    return classesAsStrings
}

fun writeModelDataClass(models: Map<String, Map<String, Any>>, packagePath: String): Map<String, String> {
    val enumImportTemplate: (String) -> String =
        { if (it == "String" || it == "Integer") "" else "\nimport $packagePath.kotlin.$it" }

    val fieldTemplate: (String, String) -> String = { fieldName: String, fieldType: String ->
        var returningFieldType = fieldType
        if (fieldType == "Integer") {
            returningFieldType = "Int"
        }
        "\tval $fieldName: $returningFieldType,"
    }

    val dataPackageHead: (String) -> String = { "package $it.kotlin.model\n" }

    val dataClassHead: (String) -> String = { "\n\ndata class $it (\n" }
    val dataClassFoot = "\n)"

    val classesAsStrings = models
        .entries.associate { entity ->
            val returnValue = dataPackageHead(packagePath) +
                    (entity.value as Map<String, *>).entries.joinToString(separator = "") {
                        enumImportTemplate(it.value as String)
                    } +
                    dataClassHead(entity.key) +
                    (entity.value as Map<String, *>).entries.joinToString(separator = "\n") {
                        fieldTemplate(it.key, it.value as String)
                    } +
                    dataClassFoot
            entity.key to returnValue
        }

    return classesAsStrings
}

fun writeJavaServiceClass(entities: Map<String, Map<String, Any>>, packagePath: String): Map<String, String> {
    val importStatements: (String) -> String = {
        "import org.springframework.beans.factory.annotation.Autowired;\n" +
        "import org.springframework.stereotype.Service;\n" +
        "import wld.accelerate.pipelinec.java.entity.$it;\n" +
        "import wld.accelerate.pipelinec.java.model.${it}Model;\n" +
        "import wld.accelerate.pipelinec.java.repository.${it}Repository;\n" +
        "import java.util.List;"
    }

    val classDeclaration: (String) -> String = {
        "@Service\npublic class ${it}Service {"
    }
    val closeClass = "}"

    val autowiredComponents: (String) -> String = {
        "\t@Autowired\n" +
                "\tpublic ${String.capitalize(it)}Repository ${it.lowercase()}Repository;\n" +
                "\n"
    }

    val packageStatement = "package $packagePath.java.service;\n"

    val findByIdMethodTemplate: (String) -> String = {
        "\tpublic $it findById(Integer id) {\n" +
                "\t\treturn ${it.lowercase()}Repository.findById(id).orElseThrow();\n" +
                "\t}"
    }

    val findAllTemplate: (String) -> String = {
        "\tpublic List<$it> findAll() {\n" +
                "\t\treturn ${it.lowercase()}Repository.findAll();\n" +
                "\t}"
    }

    val createTemplate: (String) -> String = {
        "\tpublic $it create$it(${it}Model ${String.unCapitalize(it)}Model){\n" +
                "\t\t${it} ${String.unCapitalize(it)} = ${it}Model.to$it(${String.unCapitalize(it)}Model);\n" +
                "\t\treturn ${String.unCapitalize(it)}Repository.save(${String.unCapitalize(it)});\n" +
                "\t}"
    }

    val updateTemplate: (String, Map<String, String>) -> String = { it, map ->
        "\tpublic $it update$it(Integer id, ${it}Model ${String.unCapitalize(it)}Model){\n" +
                "\t\t$it ${String.unCapitalize(it)} = findById(id);\n" +
                map.entries.joinToString(separator = "") {
                        entry -> "\t\t${it.lowercase()}.set${String.capitalize(entry.key)}(${entry.value});\n" } +
                "\t\treturn ${String.unCapitalize(it)}Repository.save(${String.unCapitalize(it)});\n" +
                "\t}"
    }

    val classesAsStrings = entities
        .entries.associate { entity ->
            val returnValue =
                packageStatement + "\n" +
                        importStatements(entity.key) + "\n" +
                        classDeclaration(entity.key) + "\n" +
                        autowiredComponents(entity.key) + "\n" +
                        findByIdMethodTemplate(entity.key) + "\n" +
                        findAllTemplate(entity.key) + "\n" +
                        createTemplate(entity.key) + "\n" +
                        updateTemplate(entity.key, entity.value.entries.associate { entry -> entry.key to "${String.unCapitalize(entity.key)}.get${String.capitalize(entry.key)}()" }) + "\n" +
                        closeClass

            entity.key to returnValue
    }

    return classesAsStrings
}

// Function to generate a Java Entity Class
fun writeEntityDataClassJava(entities: Map<String, Map<String, Any>>, packagePath: String): Map<String, String> {
    val enumImportTemplate: (String) -> String =
        { if (it == "String" || it == "Integer") "" else "\nimport $packagePath.java.$it;" }

    val defaultIdTemplate: String = "\n\t@Id\n" +
            "\t@GeneratedValue\n" +
            "\tprivate Long id = null;\n"


    val fieldTemplate: (String, String) -> String = { fieldName: String, fieldType: String ->
        "\n\t@Column(nullable = false)\n\t" +
                "$fieldType $fieldName = " +
                when (fieldType) {
                    "String" -> "\"\";\n"
                    "Enum" -> "[];\n"
                    else -> "null;\n"
                }
    }

    val getMethodTemplate: (String, String) -> String = { fieldName, fieldType ->
        "\n\tpublic $fieldType get${String.capitalize(fieldName)}(){\n\t\treturn $fieldName;\n\t}\n"
    }

    val setMethodTemplate: (String, String) -> String = { fieldName, fieldType ->
        "\n\tpublic void set${String.capitalize(fieldName)}($fieldType $fieldName) {" +
                "\n\t\tthis.$fieldName = $fieldName;\n\t}\n"
    }

    val dataImportHead = "" +
            "import java.io.Serializable;\n\n" +
            "import jakarta.persistence.Column;\n" +
            "import jakarta.persistence.Entity;\n" +
            "import jakarta.persistence.GeneratedValue;\n" +
            "import jakarta.persistence.Id;\n"

    val dataPackageHead: (String) -> String = { "package $it.java.entity;\n\n" }

    val dataClassHead: (String) -> String = { "\n@Entity\n" +
            "public class $it implements Serializable {" }
    val dataClassFoot = "\n}"

    val classesAsStrings = entities
        .entries.associate { entity ->
            val returnValue =
                dataPackageHead(packagePath) + (entity.value as Map<String, *>).entries.joinToString(separator = "") {
                    enumImportTemplate(it.value as String)
                } + "\n" + dataImportHead + dataClassHead(entity.key) + defaultIdTemplate +
                        (entity.value as Map<String, *>).entries.joinToString(separator = "\n") {
                            fieldTemplate(it.key, it.value as String)
                        } +
                        (entity.value as Map<String, *>).entries.joinToString(separator = "\n") {
                            getMethodTemplate(it.key, it.value as String)
                        } +
                        (entity.value as Map<String, *>).entries.joinToString(separator = "\n") {
                            setMethodTemplate(it.key, it.value as String)
                        } +
                        dataClassFoot

            entity.key to returnValue
        }

    return classesAsStrings
}

fun writeEntityDataClass(entities: Map<String, Map<String, Any>>, packagePath: String): Map<String, String> {
    val enumImportTemplate: (String) -> String =
        { if (it == "String" || it == "Integer") "" else "\nimport $packagePath.kotlin.$it" }

    val defaultIdTemplate: String = "\t@Id\n" +
            "\t@GeneratedValue\n" +
            "\tprivate val id: Long? = null\n"

    val fieldTemplate: (String, String) -> String = { fieldName: String, fieldType: String ->
        "\n\t@Column(nullable = false)\n\t" +
                "var $fieldName: $fieldType? = " +
                when (fieldType) {
                    "String" -> "\"\""
                    "Enum" -> "[]"
                    else -> "null"
                }
    }

    val dataImportHead = "" +
            "import java.io.Serializable\n\n" +
            "import jakarta.persistence.Column\n" +
            "import jakarta.persistence.Entity\n" +
            "import jakarta.persistence.GeneratedValue\n" +
            "import jakarta.persistence.Id\n"

    val dataPackageHead: (String) -> String = { "package $it.kotlin.entity\n\n" }

    val dataClassHead: (String) -> String = { "\n@Entity\nclass $it : Serializable {\n" }
    val dataClassFoot = "\n}"

    val classesAsStrings = entities
        .entries.associate { entity ->
            val returnValue =
                dataPackageHead(packagePath) + (entity.value as Map<String, *>).entries.joinToString(separator = "") {
                    enumImportTemplate(it.value as String)
                } + "\n" + dataImportHead + dataClassHead(entity.key) + defaultIdTemplate +
                        (entity.value as Map<String, *>).entries.joinToString(separator = "\n") {
                            fieldTemplate(it.key, it.value as String)
                        } +
                        dataClassFoot

            entity.key to returnValue
        }

    return classesAsStrings
}