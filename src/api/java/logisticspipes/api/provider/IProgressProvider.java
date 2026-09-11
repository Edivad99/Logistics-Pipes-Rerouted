package logisticspipes.api.provider;

/**
 * A machine that reports its own progress to a crafting pipe, which shows it in the crafting tree.
 *
 * <p>Implement this on your block entity when you own its class; for a machine you do not own,
 * register an {@link IGenericProgressProvider} instead.
 */
public interface IProgressProvider {

    /** @return how far along this machine is, from 0 to 100. */
    byte getMachineProgressForLP();
}
