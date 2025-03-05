import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import io.github.wadoon.key.interactionlog.InteractionLogFacade
import io.github.wadoon.key.interactionlog.model.InteractionLog
import io.github.wadoon.key.interactionlog.model.OSSBuiltInRuleInteraction
import io.github.wadoon.key.interactionlog.model.PruneInteraction
import io.github.wadoon.key.interactionlog.model.SMTBuiltInRuleInteraction
import java.io.File

/**
 * @author Alexander Weigl
 * @version 1 (06.12.18)
 */
class InteractionLogFacadeTest {
    internal fun writeAndReadInteractionLog(il: InteractionLog): InteractionLog {
        val file = File.createTempFile("interaction_log", ".json")
        InteractionLogFacade.storeInteractionLog(il, file)

        println(file)
        println(file.readText())

        assertTrue(file.exists())

        val readIl = InteractionLogFacade.readInteractionLog(file)
        assertEquals(il.interactions.size, readIl.interactions.size)
        assertEquals(il.name, readIl.name)

        return readIl
    }

    @Test
    fun storeAndReadInteractionLogEmpty() {
        val il = InteractionLog()
        writeAndReadInteractionLog(il)
    }

    @Test
    fun storeAndReadOss() {
        val il = InteractionLog()
        il.add(OSSBuiltInRuleInteraction())
        il.add(SMTBuiltInRuleInteraction())
        il.add(PruneInteraction())
        val ol = writeAndReadInteractionLog(il)
        println(ol.interactions)
        assertEquals(3, ol.interactions.size)
    }

}