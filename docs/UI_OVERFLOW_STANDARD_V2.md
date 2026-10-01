# Student OS — UI Overflow Standard v2

## Rule
Information cards are summaries, not documents.

## Title
- maxLines = 1
- overflow = Ellipsis

## Supporting text
- default maxLines = 2
- critical explanations maxLines = 3
- overflow = Ellipsis

## Long-form content
Use a bounded viewport with vertical scrolling.

Recommended bounds:
- compact summary: 96–128dp
- standard info card: 128–180dp
- AI response viewport: about 320dp
- code/callout viewport: 140–160dp

## Rows
Any row with independent content groups must give the primary text a weighted width so actions cannot be pushed outside the card.

## Inputs
Text fields remain editable and scrollable; the summary clamp must not be applied to user input.

## AI
AI output must never determine unbounded screen size. Long responses use a bounded viewport and structured rendering.

## Critical information
Never truncate the primary action or the minimum state needed to understand risk. Put extended details behind scroll or expand.

## Testing
Every reusable information component should have at least one long-text UI test.