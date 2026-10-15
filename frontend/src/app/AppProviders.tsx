import { MantineProvider } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { theme } from './theme';

type AppProvidersProps = {
  readonly queryClient: QueryClient;
  readonly children: ReactNode;
};

export function AppProviders({ queryClient, children }: AppProvidersProps) {
  return (
    <MantineProvider theme={theme}>
      <Notifications position="top-right" />
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </MantineProvider>
  );
}
