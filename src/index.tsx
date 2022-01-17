import { NativeModules, Platform, NativeEventEmitter } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-fall-detection-module' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const FallDetectionModule = NativeModules.FallDetectionModule
  ? NativeModules.FallDetectionModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export const FallDetectionEmitter = new NativeEventEmitter(FallDetectionModule);

export function setUpdateInterval(updateInterval: number) {
  FallDetectionModule.setUpdateInterval(updateInterval);
}

export function start() {
  FallDetectionModule.startUpdates();
}

export function stop() {
  FallDetectionModule.stopUpdates();
}
