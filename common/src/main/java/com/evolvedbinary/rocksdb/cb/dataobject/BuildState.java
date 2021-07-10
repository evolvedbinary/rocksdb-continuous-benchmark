package com.evolvedbinary.rocksdb.cb.dataobject;

import javax.annotation.Nullable;

/**
 * The State of the Build.
 *
 * Encodes the rules for a very simple FSM (Finite State Machine),
 * Each State knows the preceding valid state, transitions from one state
 * to another can then be checked for validity externally if desired.
 */
public enum BuildState {
    REQUESTING(null),
    REQUESTED(REQUESTING),

    UPDATING_SOURCE(REQUESTED),
    UPDATING_SOURCE_FAILED(UPDATING_SOURCE),
    UPDATING_SOURCE_COMPLETE(UPDATING_SOURCE),


    BUILDING(UPDATING_SOURCE_COMPLETE),
    BUILDING_FAILED(BUILDING),
    BUILDING_COMPLETE(BUILDING),

    BENCHMARKING(BUILDING_COMPLETE),
    BENCHMARKING_FAILED(BENCHMARKING),
    BENCHMARKING_COMPLETE(BENCHMARKING);


    @Nullable final BuildState prevBuildState;

    BuildState(final BuildState prevBuildState) {
        this.prevBuildState = prevBuildState;
    }

    /**
     * The Build State that must proceed this Build State.
     *
     * @return the preceding build state, or null if this is the first state.
     */
    public @Nullable BuildState getPrevBuildState() {
        return prevBuildState;
    }

    /**
     * Returns true if the state is an update to the current state
     * and is a successful state itself.
     *
     * @param buildState the build state to test
     *
     * @return true if the state is a success update state, false otherwise.
     */
    public static boolean isStateUpdateSuccessState(final BuildState buildState) {
        return buildState == UPDATING_SOURCE
                || buildState == UPDATING_SOURCE_COMPLETE
                || buildState == BUILDING
                || buildState == BUILDING_COMPLETE
                || buildState == BENCHMARKING;
    }

    /**
     * Returns true if the state is a failure state.
     *
     * @param buildState the build state to test
     *
     * @return true if the state is a failure state, false otherwise.
     */
    public static boolean isStateFailureState(final BuildState buildState) {
        return buildState == UPDATING_SOURCE_FAILED
                || buildState == BUILDING_FAILED
                || buildState == BENCHMARKING_FAILED;
    }

    /**
     * Returns true if the state is the final success state.
     *
     * @param buildState the build state to test
     *
     * @return true if the state is the final success state, false otherwise.
     */
    public static boolean isStateFinalSuccessState(final BuildState buildState) {
        return buildState == BENCHMARKING_COMPLETE;
    }

    /**
     * Returns true if the state is a final state, i.e. the final success state or a failure state.
     *
     * @param buildState the build state to test
     *
     * @return true if the state is a final state, false otherwise.
     */
    public static boolean isStateFinalState(final BuildState buildState) {
        return isStateFailureState(buildState) ||
                isStateFinalSuccessState(buildState);
    }
}