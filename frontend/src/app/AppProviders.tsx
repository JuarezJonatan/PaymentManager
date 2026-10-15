import { MantineProvider } from '@mantine/core';
import type { ReactNode } from 'react';
import { theme } from './theme';

type AppProvidersProps = {
  readonly children: ReactNode;
};

export function AppProviders({ children }: AppProvidersProps) {
  return <MantineProvider theme={theme}>{children}</MantineProvider>;
}
