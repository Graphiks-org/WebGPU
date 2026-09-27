config.customLaunchers = {
  ...config.customLaunchers,
  ChromeHeadlessWebGPU: {
    base: 'ChromeHeadless',
    flags: [
      '--enable-unsafe-webgpu',
      '--enable-unsafe-swiftshader',
      '--use-angle=swiftshader',
      '--disable-dev-shm-usage'
    ]
  }
};
config.browsers = ['ChromeHeadlessWebGPU'];
config.browserNoActivityTimeout = 120000;
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 120000 });
