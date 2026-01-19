const mockAxios = {
  get: jest.fn(),
};

module.exports = {
  __esModule: true,
  default: mockAxios,
  ...mockAxios,
};

