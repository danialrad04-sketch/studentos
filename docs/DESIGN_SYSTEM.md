# Student OS — Academic Premium Design System

## Brand

Primary brand identity:
- Petrol / Navy Blue
- Olive Green

Visual direction:
- Academic Premium
- Professional
- Calm
- Organized
- Modern without visual noise

Avoid:
- Neon branding
- Excessive gradients
- Generic AI SaaS styling
- Excessive glassmorphism
- Decorative surfaces without information value

## Core tokens

Implementation sources:
- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `ui/theme/Shape.kt`
- `ui/theme/Spacing.kt`
- `ui/theme/StudentOsDesignTokens.kt`

## Components

Shared primitives:
- AcademicCard
- AcademicSectionHeader
- AcademicPrimaryButton
- AcademicStatusChip
- AcademicEmptyState
- AcademicErrorState

## Accessibility

Every shared component should define:
- Accessible semantics
- Minimum interactive size
- RTL behavior
- Light/Dark behavior
- Focus/pressed/disabled behavior
- Color-independent status where applicable

## Layout

Adaptive targets:
- Phone
- Foldable
- Tablet
- Large screen

Density:
- Dashboard: balanced
- Analytics: dense
- Onboarding: spacious
- Focus: minimal

## Motion

Motion should communicate state and hierarchy. Prefer fast, purposeful transitions and respect reduced-motion preferences.

## Surface strategy

Surfaces may establish hierarchy, but cards are not mandatory for every piece of content. Use flat sections when a card adds no information value.
