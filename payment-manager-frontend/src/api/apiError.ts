export type FieldErrors = Readonly<Record<string, string>>;

type ApiErrorOptions = {
  readonly status: number | null;
  readonly title: string;
  readonly detail: string;
  readonly fieldErrors?: FieldErrors;
  readonly cause?: unknown;
};

export class ApiError extends Error {
  readonly status: number | null;
  readonly title: string;
  readonly detail: string;
  readonly fieldErrors: FieldErrors;

  constructor({ status, title, detail, fieldErrors = {}, cause }: ApiErrorOptions) {
    super(detail, { cause });
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
    this.fieldErrors = fieldErrors;
  }

  get isNetworkError(): boolean {
    return this.status === null;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

const UNKNOWN_ERROR_MESSAGE = 'An unexpected error occurred';

export function getErrorMessage(error: unknown): string {
  return isApiError(error) ? error.detail : UNKNOWN_ERROR_MESSAGE;
}

export function isNotFoundError(error: unknown): boolean {
  return isApiError(error) && error.status === 404;
}
