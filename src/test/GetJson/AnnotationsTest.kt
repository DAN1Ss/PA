package GetJson

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AnnotationsTest {
    @Test
    fun `test annotations are properly defined`() {
        // Test @RestController annotation
        val restControllerAnnotation = RestController::class.java
        assertEquals(AnnotationTarget.CLASS, restControllerAnnotation.getAnnotation(Target::class.java).allowedTargets[0])
        assertEquals(AnnotationRetention.RUNTIME, restControllerAnnotation.getAnnotation(Retention::class.java).value)

        // Test @GetMapping annotation
        val getMappingAnnotation = GetMapping::class.java
        assertEquals(AnnotationTarget.FUNCTION, getMappingAnnotation.getAnnotation(Target::class.java).allowedTargets[0])
        assertEquals(AnnotationRetention.RUNTIME, getMappingAnnotation.getAnnotation(Retention::class.java).value)

        // Test @PathVariable annotation
        val pathVariableAnnotation = PathVariable::class.java
        assertEquals(AnnotationTarget.VALUE_PARAMETER, pathVariableAnnotation.getAnnotation(Target::class.java).allowedTargets[0])
        assertEquals(AnnotationRetention.RUNTIME, pathVariableAnnotation.getAnnotation(Retention::class.java).value)

        // Test @RequestParam annotation
        val requestParamAnnotation = RequestParam::class.java
        assertEquals(AnnotationTarget.VALUE_PARAMETER, requestParamAnnotation.getAnnotation(Target::class.java).allowedTargets[0])
        assertEquals(AnnotationRetention.RUNTIME, requestParamAnnotation.getAnnotation(Retention::class.java).value)
    }
}