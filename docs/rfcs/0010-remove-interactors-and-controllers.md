---
layout: page
title: Remove Interactors and Controllers
permalink: /rfc/0010-remove-interactors-and-controllers
---

* Start date: 2022-03-27
* RFC PR: [TODO](TODO)

## Summary

Fenix's architecture should be simplified by completely replacing `Interactor`s and `Controller`s with the opinionated Redux-like `lib-state` library.

## Motivation

The `Interactor/Controller` types, as a design pattern, have recently had their usefulness and clarity come into question in reviews and conversations with some regularity. `Interactor`s in particular seem to often be an unnecessary abstraction layer, usually containing methods that call similarly named methods in a `Controller` with a name similar to the interactor. Interactors that are used solely to delegate to other classes will be referred to as 'passthrough' interactors throughout this document. 

The [definition](../../fenix/docs/architecture-overview.md#interactor) of interactors in our architecture overview indicate that this is at least partially intentional: 'Called in response to a direct user action. Delegates to something else'. This is even referenced as [limitation at the end of the overview](../../fenix/docs/architecture-overview.md#known-limitations). 

Ultimately, this design is intended to move business logic out of classes tied to the Android framework and into classes that are more testable, re-usable, composable, digestible, and handle a single responsibility. More reading on architecture goals is available [in our Architecture Decisions](../../fenix/docs/Architecture-Decisions.md#goals).

These goals are all met reasonably well by the existing pattern, but some members of the team find that the pattern also contributes to unnecessary class explosion, general code complexity, and confusion about responsibility for each architectural type. This becomes especially true when wondering how responsibility should be split between interactors and controllers. It seems that interactors are often included as a matter of precedent, leading to a large amount passthrough interactors.

### Further contextual reading

An investigation was previously done to provide [further context](https://docs.google.com/document/d/1vwERcAW9_2LkcYENhnA5Kb-jT_nBTJNwnbD5m9zYSA4/edit#heading=h.jjoifpwhhxgk) on the current state of interactors. To summarize findings _in Fenix specifically_:
1. 29/36 concrete `Interactor` classes are strict passthrough interactors or are passthrough interactors that additionally record metrics.
2. Other interactors seem to have mixed responsibilities, including but not limited to:
	- dispatching actions to `Store`s
	- initiating navigation events
	- delegating to `UseCase`s or directly to closures instead of `Controller`s.

### Goals

1. Increase code comprehensibility
2. Have clear delineation of responsibility between architectural components
3. Retain ability to manage state outside of fragments/activities/etc
4. Any accepted proposal should have the ability to be adopted incrementally
5. Retain ability to compose features

## Guide-level explanation

This proposal is intended to be a long-term investment in our code health and is suggested for the following reasons: 
- incrementally approachable
- architectural uniformity removes all questions of which component code should be placed in
- testability and reproducibility are increased when state changes are explicitly defined in `Reducer`s and when side-effects are relegated to middlewares
- it is a well-known pattern throughout the industry and may make onboarding easier
- similar side-effects can be logically grouped into a single place. for example, all telemetry could be moved into a `TelemetryMiddleware` or all navigation events could be handled in a navigation middleware
- if we additionally move towards a single Store model, then composition is baked in as a first principle

The end state of this proposal would result in the following:
- removal of all interactors
- removal of all controllers
- all actions dispatched directly from `View`s, `Fragment`s, `@Composable`s, etc.
- all state updates from `Store`s  observed directly in `View`s, `Fragment`s, `@Composable`s, etc.
- side effects handled through `Middleware`s attached to the Store

This would address all the goals listed above:
1. Code comprehensibility should be improved by having a single architectural pattern. All business logic would be discoverable within `Reducer`s, and changes to `State` would be much more explicit. 
2. Responsibility would be clearly delineated as all business logic would be handled by `Reducer`s and all side-effects by `Middleware`, instead of being scattered between those components as well as `Interactor`s and `Controller`s. 
3. `State` management would happen within `Reducer`s and would still accomplish the usual goals around testability.
4. Refactoring interactors/controllers to instead dispatch actions and react to state changes should be doable on a per-component basis.
5. Composition could be handled implicitly by moving to a single store approach, as is generally suggested by Redux libraries. See the [the Redux documentation](https://redux.js.org/tutorials/fundamentals/part-4-store#redux-store) or the derived [Redux Kotlin documentation](https://reduxkotlin.org/basics/store) for examples. A follow-up RFC will suggest changes to our architecture that would allow us to combine our existing stores and then decompose them into smaller, focused child stores as desired.

### A Brief Example

Throughout the app are examples of the Redux pattern being used immediately after navigating down a chain of interactors and controllers. Simplifying this mental model should allow faster code iteration and code navigation.

For one example, here is the code path that happens when a user long-clicks a history item:

```
// 1. in LibrarySiteItemView
setOnClickListener {
    val selected = holder.selectedItems
    when {
        selected.isEmpty() -> interactor.open(item)
        item in selected -> interactor.deselect(item)
        // "select" is the path we are following
        else -> interactor.select(item)
    }
}

// 2. Clicking into `interactor.select` will lead to SelectionInteractor<T>.
// 3. Searching for implementors of that interface will lead us to another interface, `HistoryInteractor : SelectionInteractor<History>`
// 4. DefaultHistoryInteractor implements HistoryInteractor
override fun open(item: History) {
    historyController.handleSelect(item)
}

// 5. Clicking into `handleSelect` leads to `HistoryController`
// 6. DefaultHistoryController implements `HistoryController::handleSelect`
override fun handleSelect(item: History) {
    if (store.state.mode === HistoryFragmentState.Mode.Syncing) {
        return
    }

    store.dispatch(HistoryFragmentAction.AddItemForRemoval(item))
}
```

As demonstrated, there are quite a few abstraction layers until a spot is reach where the Store is being used directly anyway. By implementing this proposal, this logic could be shifted directly to the View layer.

## Reference-level explanation

We will refactor interactors and controllers, removing them and instead replacing their usages with `lib-state`.

## Drawbacks

Redux-like patterns have a bit of a learning curve, and some problems can be more difficult to solve than others. For example, handling side-effects like navigation or loading state from disk. The benefits of streamlining our architecture should outweigh this, especially once demonstrative examples of solving these problems are common in the codebase.