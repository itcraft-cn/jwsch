import resolve from '@rollup/plugin-node-resolve';
import babel from '@rollup/plugin-babel';
import terser from '@rollup/plugin-terser';

const input = 'src/index.js';
const extensions = ['.js'];

const commonPlugins = [
  resolve({ extensions }),
  babel({ 
    babelHelpers: 'bundled',
    extensions,
    exclude: 'node_modules/**'
  })
];

export default [
  {
    input,
    output: {
      file: 'lib/jwsch.umd.js',
      format: 'umd',
      name: 'Jwsch',
      exports: 'named',
      sourcemap: true
    },
    plugins: [...commonPlugins, terser()]
  },
  {
    input,
    output: {
      file: 'lib/jwsch.esm.js',
      format: 'esm',
      sourcemap: true
    },
    plugins: commonPlugins
  },
  {
    input,
    output: {
      file: 'lib/jwsch.cjs.js',
      format: 'cjs',
      exports: 'named',
      sourcemap: true
    },
    plugins: commonPlugins
  }
];