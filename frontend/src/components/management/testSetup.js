// jsdom has no layout observer; Element Plus uses this browser boundary.
globalThis.ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
