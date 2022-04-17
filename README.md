# react-native-fall-detection-module

Fall Detection Library

When using a typical react native sensors library to try to detect a fall, we will need to set the update interval to < 100ms. However, this will cause the react native bridge to be overloaded. As such, the processing of the sensors data cannot be done in the React Native code.
As a solution, this library will only send events through the react native bridge when a fall is detected.

## Compatibility

Compatible with android only. If anyone would like to contribute to the IOS version of the code, please feel free to contribute to this project!

## Installation

```sh
npm install react-native-fall-detection-module
```

## Usage

```js
import {
  FallDetectionEmitter,
  start,
} from 'react-native-fall-detection-module';
// ...

  const [data, setData] = React.useState<any | undefined>();

  React.useEffect(() => {
    start();
  }, []);

  React.useEffect(() => {
    FallDetectionEmitter.addListener('fall', (newData: any) => {
      console.log(newData);
      setData(newData);
      // put your data processing step here
    });
  }, []);
```

Data format being sent when a fall is detected is 
```
{"detected" : true}
```
otherwise no data is being sent

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

# Acknowledgement
This library is adapted from [here](https://github.com/altermarkive/experimental-fall-detector-android-app)