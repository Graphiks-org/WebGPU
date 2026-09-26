import generator.CheckMissingDocumentationTask
import generator.GenerateBindingTask
import generator.LLMDocGeneratorTask
import generator.RefreshDocumentationFromSpecTask
import generator.TransformJsonDocToYamlTask

tasks.register<GenerateBindingTask>("generate-binding")
tasks.register<LLMDocGeneratorTask>("generate-doc-from-llm")
tasks.register<RefreshDocumentationFromSpecTask>("refresh-documentation-from-spec")
tasks.register<CheckMissingDocumentationTask>("check-missing-doc")
tasks.register<TransformJsonDocToYamlTask>("tranform-json-doc-to-yaml")
