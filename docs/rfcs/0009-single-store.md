---
layout: page
title: Switching Fenix to a single Store
permalink: /rfc/0009-single-store
---

* Start date: 2022-03-27
* RFC PR: [TODO](TODO)

## Summary

Fenix's usage of `Store`s should be simplified to follow the traditional Redux pattern of using a single Store. Furthermore, there should be introduced a method which can scope a Store to smaller child Stores as needed in order to create manageably-sized mental models.

## Motivation

As the number of `Store`s grow, the following issues are exacerbated:

- need prior knowledge of more `State` types and which `Store`s they are available in
- need prior knowledge of which `Store` an action should be dispatched on
- dependency injection becomes more complex
- cases where multiple Stores are required to get and update the state of a single feature 
- complexity in the ordering of initialization and state reductions

By combining our `Store`s, we can achieve the following benefits:

- decrease cognitive overhead
- ease of use
- model changes to the application's entire state in linear, reproducible manner
    - this can have a positive impact in things like debugging and logging

### Split States, not Stores

In a typical implementation following a pattern like Redux, there is only one app-wide Store,which manages the state of the entire application. As features are introduced and the size of a Store's state grows larger, is is usually split into child states and their relevant reducers. For example:

```
data class TopLevelState(  
    val settingsState: SettingsState,  
    val experimentState: ExperimentState,  
)  
  
data class SettingsState(  
    val pref: Boolean  
)  
  
data class ExperimentState(  
    val ABTestEnabled: Boolean  
)
fun topLevelReducer(state: TopLevelState, action: Action): TopLevelState =  
    when (action) {  
        is SettingsAction -> state.copy(  
            settingsState = settingsReducer(state.settingsState, action)  
        )  
        is ExperimentAction -> state.copy(  
            experimentState = experimentReducer(state.experimentState, action)  
        )  
        else -> state  
    }  
  
fun settingsStateReducer(state: SettingsState, action: SettingsAction): SettingsState
  
fun experimentStateReducer(state: ExperimentState, action: ExperimentAction): ExperimentState
```

This keeps the size of the code manageable, and has the added benefits of showing the composition of different features explicitly by clearly defining related actions and state.

In Fenix, we have two main Stores: the `AppStore` and the `BrowserStore`. This is due to our goal of shipping Android Components as a standalone starter kit for a browser, so that consumers of the library can use `BrowserStore` to manage their browser state easily. There are several smaller Stores as well, like the `SyncStore` and `TabsTrayStore`.

## Guide-level explanation

There are options for how to implement this. The simplest, most incrementally adoptable approach could be the following:

1. Change `Store.state` to be open
2. Change `Store::dispatch` to be open
3. Create a package private `ScopedStore` class which will delegate state accesses and dispatches to its parent store
4. Create a top-level Store in Fenix that is generic on `Action` instead of a subtype of `Action`.

This would allow us to create scoped stores that can adhere to existing types without introducing breaking changes for downstream consumers of Android Components. For example:

```
typealias BrowserStore = Store<BrowserState, BrowserAction>

// browserStore will now only contain BrowserState and can only have BrowserActions dispatched to it.
val browserStore = AppStore.scope(reducer = ::browserStateReducer) { appState.browserState }
```

This workflow has been prototyped using the following new additions:
```
private class ScopedStore<ParentState : State, ChildState : State, ChildAction : Action>(
    private val superStore: Store<ParentState, Action>,
    stateScoper: StateScoper<ParentState, ChildState, ChildAction>,
    reducer: Reducer<ChildState, ChildAction>,
    middleware: List<Middleware<ChildState, ChildAction>> = emptyList(),
) : Store<ChildState, ChildAction>(stateScoper.stateMapper(), reducer, middleware) {
    override val state: ChildState by stateScoper

    override fun dispatch(action: ChildAction) {
        return superStore.dispatch(action)
    }
}

private class StateScoper<ParentState: State, ChildState: State, ChildAction: Action>(val stateMapper: () -> ChildState) {
    operator fun getValue(thisRef: ScopedStore<ParentState, ChildState, ChildAction>?, property: KProperty<*>): ChildState {
        return stateMapper()
    }
}

private class ScopedStore<ParentState : State, ChildState : State, ChildAction : Action>(
    private val parentStore: Store<ParentState, Action>,
    scoper: StateScoper<ParentState, ChildState, ChildAction>,
    reducer: Reducer<ChildState, ChildAction>,
    middleware: List<Middleware<ChildState, ChildAction>> = emptyList(),
) : Store<ChildState, ChildAction>(scoper.stateMapper(), reducer, middleware) {
    override val state: ChildState by scoper

    override fun dispatch(action: ChildAction) {
        return parentStore.dispatch(action)
    }
}
```

This would allow state and actions to be clearly defined in a per-feature/module/area basis.

There are things to note here:
1. The resulting scoped store is generic on a subtype bound by Action (`ChildAction : Action`).
2. The parent store is generic on a unbound Action (just `Action`).

Scoped stores could be arbitrarily nested if we chose to make them generic on unbounded actions. This would mean that we lose compile-time exhaustiveness checking of actions and we would not be able to use typealiases of existing Store definitions, which would require updates to many extension functions etc. 

### Alternative implementations

1. Change the definition of `Store` such that it was not generic on a subtype of Action. 
Pros
- Scoped stores could be arbitrarily nested easily
- All Stores would behave the same way
- May not require opening `Store.state` and `Store::dispatch`
Cons
- Would require more upfront work
- Introduces breaking changes in Android Components

2. Define Actions for every parent and child store and mappings between them.
Pros
- Would require no changes to Android Components
Cons
- Extreme amounts of boilerplate. Likely to end up more confusing than helpful.

## Prior art
- [Store Scoping in Swift Composable Architecture](https://pointfreeco.github.io/swift-composable-architecture/main/documentation/composablearchitecture/store/scope(state:action:)/)
- [Store slicing in Redux Toolkit](https://redux-toolkit.js.org/api/createSlice)