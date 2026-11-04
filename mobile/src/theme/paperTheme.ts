import { MD3DarkTheme, MD3LightTheme, type MD3Theme } from 'react-native-paper';
import { colors, type AppColorScheme } from './tokens';

/**
 * Chuyển design tokens sang theme Material 3 của React Native Paper.
 * @reference docs/plan-app/01-phase-1.md §4.1
 */
export function getPaperTheme(scheme: AppColorScheme): MD3Theme {
  const base = scheme === 'dark' ? MD3DarkTheme : MD3LightTheme;
  const c = colors[scheme];

  return {
    ...base,
    roundness: 14,
    colors: {
      ...base.colors,
      primary: c.primary,
      onPrimary: c.onPrimary,
      primaryContainer: c.primaryContainer,
      onPrimaryContainer: c.onPrimaryContainer,
      secondary: c.accent,
      onSecondary: '#FFFFFF',
      secondaryContainer: c.primaryContainer,
      onSecondaryContainer: c.onPrimaryContainer,
      surface: c.surface,
      onSurface: c.onSurface,
      surfaceVariant: c.surfaceVariant,
      onSurfaceVariant: c.onSurfaceVariant,
      outline: c.outline,
      outlineVariant: c.outline,
      error: c.error,
      onError: c.onError,
      background: c.surface,
      onBackground: c.onSurface,
    },
  };
}
