import { QueryClient } from '@tanstack/react-query';
import { render as testingLibraryRender, type RenderOptions } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router';
import { AppProviders } from '@/app/AppProviders';
import { routes } from '@/app/routes';

type RenderWithProvidersOptions = Omit<RenderOptions, 'wrapper'> & {
  readonly route?: string;
};

function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
      mutations: { retry: false },
    },
  });
}

function renderRouter(
  routeObjects: RouteObject[],
  route: string,
  options?: Omit<RenderOptions, 'wrapper'>,
) {
  const router = createMemoryRouter(routeObjects, { initialEntries: [route] });
  const queryClient = createTestQueryClient();

  return {
    user: userEvent.setup(),
    router,
    queryClient,
    ...testingLibraryRender(
      <AppProviders queryClient={queryClient}>
        <RouterProvider router={router} />
      </AppProviders>,
      options,
    ),
  };
}

export function render(ui: ReactElement, options: RenderWithProvidersOptions = {}) {
  const { route = '/', ...renderOptions } = options;
  return renderRouter([{ path: '*', element: ui }], route, renderOptions);
}

export function renderApp(route = '/', options?: Omit<RenderOptions, 'wrapper'>) {
  return renderRouter(routes, route, options);
}

export * from '@testing-library/react';
