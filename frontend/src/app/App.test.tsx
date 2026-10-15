import { render, screen } from '../../tests/render';
import { App } from './App';

describe('App', () => {
  it('given the app is loaded, when it renders, then shows the application title', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Payment Manager' })).toBeInTheDocument();
  });
});
