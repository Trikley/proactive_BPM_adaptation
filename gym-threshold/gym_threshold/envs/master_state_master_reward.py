import os

import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
from gym import spaces
from gym_threshold.envs.baseenv import BaseEnv, get_average_last_entries_from_numeric_list


class MasterStateMasterReward(BaseEnv):
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

        # state = rel. position, reliability, pred. deviation
        low_array = np.array([0., 0., np.finfo(np.float32).min])
        high_array = np.array([1., np.finfo(np.float32).max, np.finfo(np.float32).max])
        self.observation_space = spaces.Box(low=low_array, high=high_array)

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
                       process_length, actual_duration=0.):
        if not done:
            reward = 0
        else:
            alpha = ((1. - 0.5) / process_length) * position
            violation = actual_duration > planned_duration
            if adapted:
                if violation:
                    reward = alpha
                else:
                    reward = -0.5 - alpha * 0.5
            else:
                if violation:
                    reward = -1.
                else:
                    reward = 1.
        return reward

    def reset(self):
        self.send_action(-1)
        self.receive_reward_and_state()

        self.state = self._create_state()

        return self.state

    def _create_state(self):
        relative_position = self.position / self.process_length
        prediction_deviation = (self.predicted_duration - self.planned_duration)/self.planned_duration
        self.state = np.array(
            [relative_position, self.reliability, prediction_deviation])
        return self.state

    def render(self, mode='human'):
        # we don't need this
        return
