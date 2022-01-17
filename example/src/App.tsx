import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import {
  FallDetectionEmitter,
  start,
} from 'react-native-fall-detection-module';

export default function App() {
  const [data, setData] = React.useState<any | undefined>();

  React.useEffect(() => {
    start();
  }, []);

  React.useEffect(() => {
    FallDetectionEmitter.addListener('fall', (newData: any) => {
      console.log(newData);
      setData(newData);
    });
  }, []);

  return (
    <View style={styles.container}>
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
