package de.jexcellence.oneblock.utility.workload;

/**
 * Interface for workload tasks that can be processed by the distributed workload system.
 * Implementations should represent discrete units of work that can be executed asynchronously.
 */
public interface IWorkload {
    
    /**
     * Computes this workload task.
     * This method should contain the actual work to be performed.
     */
    void compute();
    
    /**
     * Checks if this workload has been completed.
     * 
     * @return true if the workload is complete, false otherwise
     */
    default boolean isComplete() {
        return false;
    }
    
    /**
     * Gets the priority of this workload.
     * Higher values indicate higher priority.
     * 
     * @return the priority value (default is 0)
     */
    default int getPriority() {
        return 0;
    }
}
