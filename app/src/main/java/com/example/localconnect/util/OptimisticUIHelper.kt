package com.example.localconnect.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Helper for implementing optimistic UI updates
 *
 * Optimistic UI immediately reflects user actions in the UI before server confirmation,
 * making the app feel instantly responsive. If the server operation fails, the change is reverted.
 *
 * Benefits:
 * - App feels 10x more responsive
 * - Better user engagement
 * - Reduced perceived latency
 */
object OptimisticUIHelper {

    /**
     * Execute an optimistic update
     *
     * @param optimisticUpdate Function to immediately update the UI
     * @param serverOperation Suspend function for the actual server operation
     * @param revertUpdate Function to revert UI if server operation fails
     * @param onSuccess Optional callback on successful server operation
     * @param onError Optional callback on error with error message
     */
    fun <T> executeOptimistic(
        scope: CoroutineScope,
        optimisticUpdate: () -> Unit,
        serverOperation: suspend () -> T,
        revertUpdate: () -> Unit,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // 1. Immediately update UI
        optimisticUpdate()

        // 2. Execute server operation in background
        scope.launch {
            try {
                val result = serverOperation()
                // Success - UI already updated, just call success callback if provided
                onSuccess?.invoke(result)
            } catch (e: Exception) {
                // 3. Revert UI on failure
                revertUpdate()
                // 4. Notify error
                onError?.invoke(e.message ?: "Operation failed")
            }
        }
    }

    /**
     * Execute optimistic update for simple toggle operations (like/unlike)
     */
    fun executeToggle(
        scope: CoroutineScope,
        currentState: Boolean,
        updateState: (Boolean) -> Unit,
        serverOperation: suspend (Boolean) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val newState = !currentState
        executeOptimistic(
            scope = scope,
            optimisticUpdate = { updateState(newState) },
            serverOperation = { serverOperation(newState) },
            revertUpdate = { updateState(currentState) },
            onError = onError
        )
    }

    /**
     * Execute optimistic increment/decrement (for counters like likes, views)
     */
    fun executeCounter(
        scope: CoroutineScope,
        currentValue: Int,
        increment: Int,
        updateValue: (Int) -> Unit,
        serverOperation: suspend (Int) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val newValue = (currentValue + increment).coerceAtLeast(0)
        executeOptimistic(
            scope = scope,
            optimisticUpdate = { updateValue(newValue) },
            serverOperation = { serverOperation(newValue) },
            revertUpdate = { updateValue(currentValue) },
            onError = onError
        )
    }
}

/**
 * Extension function for easier optimistic UI in ViewModels
 */
fun <T> CoroutineScope.optimistic(
    update: () -> Unit,
    operation: suspend () -> T,
    revert: () -> Unit,
    onSuccess: ((T) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    OptimisticUIHelper.executeOptimistic(
        scope = this,
        optimisticUpdate = update,
        serverOperation = operation,
        revertUpdate = revert,
        onSuccess = onSuccess,
        onError = onError
    )
}

