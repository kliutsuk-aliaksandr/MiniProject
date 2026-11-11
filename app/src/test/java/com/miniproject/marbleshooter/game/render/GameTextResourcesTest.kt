package com.miniproject.marbleshooter.game.render

import com.miniproject.marbleshooter.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class GameTextResourcesTest {
    @Test
    fun russianAndEnglishResourceFilesHaveIdenticalKeys() {
        val root = File("src/main/res")
        val russian = readKeys(File(root, "values/strings.xml"))
        val english = readKeys(File(root, "values-en/strings.xml"))
        assertTrue("Russian strings.xml must exist", russian.isNotEmpty())
        assertTrue("English strings.xml must exist", english.isNotEmpty())
        assertEquals("Every translation must have a matching key", russian, english)
    }

    @Test
    fun selectedLanguageOverridesTheDeviceLocale() {
        val text = GameTextResources(RuntimeEnvironment.getApplication())
        assertEquals("НАСТРОЙКИ", text.getString("ru", R.string.settings))
        assertEquals("SETTINGS", text.getString("en", R.string.settings))
        assertEquals("Уровень 7", text.getString("ru", R.string.a11y_level, 7))
        assertEquals("Level 7", text.getString("en", R.string.a11y_level, 7))
    }

    private fun readKeys(file: File): Set<String> {
        require(file.isFile) { "Missing resource file: ${file.path}" }
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            setExpandEntityReferences(false)
        }
        val root = factory.newDocumentBuilder().parse(file).documentElement
        val keys = linkedSetOf<String>()
        val nodes = root.childNodes
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE && node.nodeName == "string") {
                keys += node.attributes.getNamedItem("name").nodeValue
            }
        }
        return keys
    }
}
