import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import {
  multiply,
  FallDetectionEmitter,
  start,
} from 'react-native-fall-detection-module';

export default function App() {
  const [result, setResult] = React.useState<number | undefined>();
  const [data, setData] = React.useState<any | undefined>();

  React.useEffect(() => {
    multiply(3, 7).then(setResult);
    start();
  }, []);

  React.useEffect(() => {
    FallDetectionEmitter.addListener('fall', (data: any) => {
      console.log(data);
      setData(data);
    });
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
      <Text>Data: {JSON.stringify(data)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
