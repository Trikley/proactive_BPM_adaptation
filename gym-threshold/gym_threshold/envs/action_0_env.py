import os

import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
from gym import spaces
from gym_threshold.envs.baseenv import BaseEnv, get_average_last_entries_from_numeric_list


class ActionOEnv(BaseEnv):
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

        # state = rel. position, reliability
        low_array = np.array([0., 0.])
        high_array = np.array([1., 2.])
        self.observation_space = spaces.Box(low=low_array, high=high_array)

    def step(self, action=None):
        action = 0

        self.send_action(int(action))
        self.action_value = action

        self.receive_reward_and_state()

        self.do_logging(action)

        info = {}
        if self.process_length == 0:
            relative_position = 0
        else:
            relative_position = (self.position / self.process_length)
        self.state = np.array([relative_position, self.reliability])

        return self.state, self.reward, self.done, info

    def compute_reward(self, adapted, cost, done, predicted_duration, planned_duration, reliability, position,
                       process_length):
        if done:
            reward = -cost
        else:
            reward = -(50. / process_length)
        return reward

    def reset(self):
        self.send_action(-1)
        self.reward, self.done, self.predicted_duration, self.planned_duration, self.reliability, self.position, \
        self.process_length, self.cost, self.adapted = self.receive_reward_and_state()

        if self.process_length == 0:
            relative_position = 0
        else:
            relative_position = (self.position / self.process_length)
        self.state = np.array([relative_position, self.reliability])

        return self.state

    def render(self, mode='human'):
        # we don't need this
        return

    def save_metrics(self):
        x = np.arange(len(self.cost_list))
        y = np.array(self.cost_list)
        sns.set_style("ticks")
        plt.plot(x, y)
        plt.legend(['action', 'reward', 'cost'], ncol=1, loc='upper left')
        plt.title("step penalty: " + " -(50./process_length)")
        plt.savefig("./results.png")
        plt.show()
