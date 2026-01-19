import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

jest.mock('axios');

test('renders without crashing', () => {
  const App = require('./App').default;
  render(
    <MemoryRouter>
      <App />
    </MemoryRouter>
  );
});
