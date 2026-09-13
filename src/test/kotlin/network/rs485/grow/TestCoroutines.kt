package network.rs485.grow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import logisticspipes.utils.ServerTickExecutor

/**
 * The integration tests still drive their steps with coroutines; production code does not.
 *
 * The dispatcher is the server tick executor, so a test step scheduled here runs inside the server
 * tick, where it may touch the world.
 */
object TestCoroutines {
    val serverDispatcher = ServerTickExecutor.INSTANCE.asCoroutineDispatcher()
    val serverScope get() = CoroutineScope(serverDispatcher)
}
