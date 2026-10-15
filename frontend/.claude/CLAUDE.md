# CLAUDE.md

This file defines the mandatory conventions for `payment-manager-frontend`. All generated or modified code under `frontend/` MUST comply with these rules. The backend conventions in `.claude/CLAUDE.md` do NOT apply here, except where this file explicitly mirrors them.

## Project Context

- Project name: `payment-manager-frontend` (folder `frontend/` inside the `payment-manager` repository)
- Language: TypeScript in `strict` mode
- UI library: React (single-page application, client-side rendering only, no SSR)
- Build tool: Vite, with npm as the package manager (Node 24)
- UI components and styling: Mantine
- Server state: TanStack Query
- Routing: React Router (library mode)
- Forms and validation: react-hook-form + zod
- Testing: Vitest + Testing Library + MSW

Exact versions are defined in `package.json`, which is the single source of truth.

### Common Commands

```bash
npm install          # install dependencies
npm run dev          # start the dev server
npm run test         # run unit and component tests (Vitest)
npm run lint         # run ESLint
npm run build        # type-check and build for production
```

Every change MUST pass `npm run lint`, `npm run test`, and `npm run build` before it is merged.

## 1. Folder Structure

Code is organized **by feature**, not by technical type:

```
src/
├── api/                  # HTTP layer: fetch client, error type, endpoint functions
├── features/
│   └── <feature>/
│       ├── hooks/        # TanStack Query hooks (queries and mutations)
│       ├── components/   # presentational components of the feature
│       ├── pages/        # route-level components that orchestrate hooks and components
│       ├── schemas.ts    # zod schemas of the feature's forms
│       └── queryKeys.ts  # query key factory of the feature
├── shared/
│   ├── components/       # cross-feature components
│   └── lib/              # cross-feature utilities
└── app/                  # router, providers, theme, and the root App component
tests/
├── setup.ts              # global test setup
├── render.tsx            # custom render that wraps components in the app providers
└── mocks/                # MSW handlers and test data builders
```

`shared` holds only code used by more than one feature. Feature-specific code MUST NOT be placed in `shared`.

## 2. Layering and Dependencies

The dependency direction MUST be **pages → hooks → api**.

| Layer | Responsibility | Allowed Dependencies |
|-------|----------------|----------------------|
| **api** | Perform HTTP requests and translate error responses into a single error type. No React code. | — |
| **hooks** | Wrap api functions in TanStack Query queries and mutations, including cache updates and invalidation. | api, queryKeys |
| **pages** | Read route and URL state, call hooks, handle loading/error/empty states, and compose components. | hooks, components, shared |
| **components** | Render data received through props and emit events through callbacks. | shared |

Rules:

- Presentational components MUST NOT call hooks from `hooks/`, TanStack Query, or `fetch`. They receive data and callbacks through props.
- All HTTP access goes through `src/api`; no other layer calls `fetch` directly.
- The browser calls relative URLs; the backend base URL MUST NOT be hardcoded in the code.
- Every non-2xx response and every network failure MUST be converted into one error type defined in `src/api`, so the rest of the app handles errors in a single way.
- Use named exports only. Default exports are forbidden, except where a tool requires them (e.g., config files).
- Use function components only.

### API Client

- Endpoint functions MUST call `request<T>(path, options)` from `src/api/httpClient.ts`, passing a path relative to the API (e.g., `/payments`). The client prepends `/api`, serializes the body as JSON, and parses the response.
- In development, the Vite dev server proxies `/api/*` to the backend and strips the `/api` prefix. The target defaults to `http://localhost:8080` and can be overridden with the `API_PROXY_TARGET` environment variable.
- The backend reports errors as `ProblemDetail` (RFC 9457). `request` converts every failure into an `ApiError`:
  - `status`: the HTTP status, or `null` for a network failure (`isNetworkError`).
  - `title` and `detail`: taken from the `ProblemDetail`; `detail` is the message to show to the user.
  - `fieldErrors`: the `errors` map of a validation failure (`field → message`), used with `setError` in forms.
- Code outside `src/api` MUST narrow errors with `isApiError` and MUST NOT parse response bodies or read `Response` objects.

## 3. State Management

| Kind of state | Where it lives |
|---------------|----------------|
| Server data | TanStack Query cache |
| Filters and pagination | URL search params (`useSearchParams`) |
| Form values and errors | react-hook-form |
| Local UI state (open modal, etc.) | `useState` or `useDisclosure` in the owning component |

Rules:

- Server data MUST NOT be copied into `useState`.
- Query keys MUST be built exclusively through the feature's `queryKeys.ts` factory; inline array keys are forbidden.
- After a mutation, update or invalidate the affected queries so the UI never shows stale data.

## 4. Forms and Validation

- Every form MUST be validated with a zod schema connected through `zodResolver`. The form type MUST be inferred with `z.infer`; do NOT declare it separately.
- Validation errors returned by the server MUST be shown next to the matching field with `setError`. Errors without a matching field are shown as a form-level message.
- Submit buttons MUST be disabled while the request is pending.

## 5. UI and UX

- Use Mantine components and layout primitives. Do NOT add another component library, Tailwind, or a CSS-in-JS solution.
- Styling MUST use, in this order of preference: component props (`variant`, `color`, `size`), Mantine style props (`mt`, `p`, `c`, etc.), and CSS Modules (`<Component>.module.css`) for anything else. Inline `style` objects are forbidden.
- Colors, spacing, and fonts MUST come from the theme defined in `src/app/theme.ts`; do NOT hardcode values.
- Every data view MUST handle its loading, error, and empty states explicitly.
- User feedback for mutations MUST use Mantine notifications.
- Destructive or irreversible actions MUST ask for confirmation before running.
- Interactive elements MUST be accessible: use semantic elements, associate every input with a label, and keep keyboard navigation working.
- Formatting of numbers, currencies, and dates MUST go through `src/shared/lib`; do NOT call `Intl` inline in components.

## 6. Testing (BDD)

Tests follow the same BDD principles as the backend.

- Test names MUST describe the scenario with the **given / when / then** structure: `describe` names the unit under test, and each `it` uses the format `given <precondition>, when <action>, then <expected outcome>`.
  - Example: `it('given an empty required field, when the form is submitted, then shows a validation error and sends no request')`
- Comments such as `// given`, `// when`, `// then` (or `// arrange`, `// act`, `// assert`) are **forbidden**. Phases MUST be separated only by a blank line.
- Each test MUST verify a single behavior.
- Query elements the way a user finds them: `getByRole`, `getByLabelText`, `getByText`. `getByTestId` is a last resort.
- Simulate user interaction with `@testing-library/user-event`, not `fireEvent`.
- Render components with the custom `render` from `tests/render.tsx`, never with Testing Library's `render` directly, so every test runs inside the app providers.
- Mock the network with MSW handlers. Do NOT mock `fetch`, api functions, or TanStack Query hooks.
  - Default handlers live in `tests/mocks/handlers.ts`; a test overrides them with `server.use(...)`, and they are reset after each test.
  - Unhandled requests fail the test (`onUnhandledFrame: 'error'`), so every request a test triggers MUST have a handler.
  - Handlers match the URL the browser calls, including the `/api` prefix (e.g., `http.get('/api/payments', ...)`).
- Failure scenarios MUST assert that the UI state did not change, not only that an error message appeared.
- Unit and component test files live next to the code they test, named `<Name>.test.ts(x)`. Shared setup and MSW handlers live in `tests/`.

## 7. General Code Style

- `any` is forbidden. Use `unknown` and narrow it.
- Code MUST be self-explanatory through clear naming; avoid redundant comments.
- Prefer immutability: `const`, `readonly` props, no mutation of props or cached data.
- File names: `PascalCase.tsx` for components, `camelCase.ts` for everything else. Hooks start with `use`.
