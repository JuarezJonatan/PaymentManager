import { renderApp, screen } from '../../tests/render';

describe('routes', () => {
  it('given the root path, when the app renders, then redirects to the payments list inside the layout', async () => {
    const { router } = renderApp('/');

    expect(await screen.findByRole('heading', { level: 1, name: 'Payments' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Payment Manager' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/payments');
  });

  it('given an unknown path, when the app renders, then shows the not found page', async () => {
    renderApp('/unknown');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Page not found' }),
    ).toBeInTheDocument();
  });

  it('given the not found page, when the user clicks go to home, then navigates to the payments list', async () => {
    const { user, router } = renderApp('/unknown');

    await user.click(await screen.findByRole('link', { name: 'Go to home' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Payments' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/payments');
  });
});
