# react-native-fall-detection-module

Fall Detection Library

To prevent the overloading of the react native bridge with the need to update the sensors value when trying to detect a fall using a typical acceleration and gyroscope sensor library, this library will only send events through the react native bridge when a fall is detected

## Disclaimer
This library may not work

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