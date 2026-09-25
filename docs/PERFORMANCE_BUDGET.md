# Student OS — Performance Budget

The performance budget is a release gate, not a suggestion.

## Measurements

Measure on a representative low/mid-range Android device and a recent high-end device.

### Startup
- Cold start
- Warm start
- Time to first useful frame

### Rendering
- Jank on Dashboard
- Jank during navigation
- Jank while scrolling long academic lists
- Animation smoothness

### Memory
- Baseline process memory
- Peak memory on Dashboard
- Peak memory on heavy lists
- Leak checks after repeated navigation

### Network
- Startup network calls
- Duplicate requests
- Background synchronization frequency
- Copilot request cost

### Package
- Release APK size
- Release AAB size
- Resource growth per release

## Release rule

A regression is investigated before release when it materially worsens an established baseline.

Exact numeric budgets are defined from measured project/device baselines rather than arbitrary targets.
