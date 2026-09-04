/**
 * Messages that mean the same thing in both directions.
 *
 * <p>Most settings travel as a pair: the client asks for a change, the server answers with the
 * state. A few are simpler than that -- applying them is one call that either side can make, and
 * the server's answer would be byte for byte the message it just received. Those live here and are
 * registered with {@code playBidirectional}, so that "apply this setting" is written once instead
 * of once per direction.
 *
 * <p>This is not the place for anything whose meaning depends on who sent it. A message that means
 * "change by this much" one way and "the value is now this" the other way is two messages, and
 * belongs in {@code to_server} and {@code to_client}.
 */
@NullMarked
package logisticspipes.network.bidirectional;

import org.jspecify.annotations.NullMarked;
