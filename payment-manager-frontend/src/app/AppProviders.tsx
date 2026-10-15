import { MantineProvider, type MantineThemeOverride } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { theme as appTheme } from './theme';

type AppProvidersProps = {
  readonly queryClient: QueryClient;
  readonly theme?: MantineThemeOverride;
  readonly children: ReactNode;
};

export function AppProviders({ queryClient, theme = appTheme, children }: AppProvidersProps) {
  return (
    <MantineProvider theme={theme}>
      <Notifications position="top-right" />
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </MantineProvider>
  );
}
