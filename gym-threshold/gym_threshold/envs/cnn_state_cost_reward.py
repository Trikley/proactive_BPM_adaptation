from stable_baselines.a2c.utils import conv, linear, conv_to_fc
import numpy as np
import tensorflow as tf
from gym import spaces
from gym_threshold.envs.baseenv import BaseEnv


def custom_cnn(scaled_images, **kwargs):
    """
    :param scaled_images: (TensorFlow Tensor) Image input placeholder
    :param kwargs: (dict) Extra keywords parameters for the convolutional layers of the CNN
    :return: (TensorFlow Tensor) The CNN output layer
    """
    activ = tf.nn.relu
    layer_1 = activ(
        conv(scaled_images, 'c1', n_filters=32, filter_size=(1, 4), stride=2, init_scale=np.sqrt(2), **kwargs))
    layer_2 = activ(conv(layer_1, 'c2', n_filters=64, filter_size=(1, 3), stride=1, init_scale=np.sqrt(2), **kwargs))
    layer_3 = conv_to_fc(layer_2)
    return activ(linear(layer_3, 'fc1', n_hidden=256, init_scale=np.sqrt(2)))


class CNNStateCostReward(BaseEnv):
    metadata = {'render.modes': ['human']}
    summary_writer = None

    def __init__(self):
        super().__init__()
        #################################
        # Parameter fuer das Environment
        #################################
        self.state = None
        self.action_space = spaces.Discrete(2)  # set action space to adaption true or false
        # Hier dimensionen des state-arrays anpassen:
        #        low_array = np.array([0, 0, 0])
        #        high_array = np.array([np.finfo(np.float32).max, np.finfo(np.float32).max, np.finfo(np.float32).max])

        self.observation_space = spaces.Box(low=0., high=np.finfo(np.float32).max,
                                            shape=(3, 16, 1), dtype=np.float32)
        self._reset_state()

    def step(self, action=None):
        if action is None:
            action = 0

        self.send_action(int(action))
        self.action_value = action

        self.receive_reward_and_state()

        self.do_logging(action)

        info = {}

        self.state = self._create_state()
        return self.state, self.reward, self.done, info

    def compute_reward(self, adapted, cost, done, predicted_duration, planned_duration, reliability, position,
                       process_length):
        reward = -cost
        return reward

    def reset(self):
        self.send_action(-1)
        self.receive_reward_and_state()

        self._reset_state()
        self.state = self._create_state()
        return self.state

    def _reset_state(self):
        self.relative_positions = np.zeros(16).tolist()
        self.reliabilities = np.zeros(16).tolist()
        self.predicted_durations = np.zeros(16).tolist()

    def _create_state(self):
        relative_position = self.position / self.process_length
        self.relative_positions.append(relative_position)
        self.relative_positions = self.relative_positions[-16:]
        self.reliabilities.append(self.reliability)
        self.reliabilities = self.reliabilities[-16:]
        self.predicted_durations.append(self.predicted_duration)
        self.predicted_durations = self.predicted_durations[-16:]
        self.state = np.array(
            [self.relative_positions, self.reliabilities, self.predicted_durations])
        self.state = np.expand_dims(self.state, 2)
        return self.state

    def render(self, mode='human'):
        # we don't need this
        return
