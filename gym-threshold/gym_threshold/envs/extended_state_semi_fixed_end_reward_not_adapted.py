import os

import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
from gym import spaces
from gym_threshold.envs.baseenv import BaseEnv, get_average_last_entries_from_numeric_list


class ExtendedStateSemiFixedEndNotAdapted(BaseEnv):
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
        low_array = np.array([0., 0., 0.])
        high_array = np.array([1., np.finfo(np.float32).max, 2.])
        self.observation_space = spaces.Box(low=low_array, high=high_array)

        self.step_penalty = -1.5
        self.episode_lengths_buffer = []
        self.max_el_buffer_size = 1000
        self.last_episode_buffered = -1

    def step(self, action=None):
        if action is None:
            action = 0

        self.send_action(int(action))
        self.action_value = action

        self.receive_reward_and_state()

        self.do_logging(action)

        info = {}
        relative_position = self.position / self.process_length
        self.state = np.array(
            [relative_position, self.reliability, self.predicted_duration])

        return self.state, self.reward, self.done, info

    def compute_reward(self, adapted, cost, done, predicted_duration, planned_duration, reliability, position,
                       process_length):
        self.calculate_step_penalty(process_length)
        if done:
            if adapted:
                reward = -cost
            else:
                self.rewards_per_episode.append(-cost)
                reward = sum(self.rewards_per_episode)
            self.rewards_per_episode = []
        else:
            self.rewards_per_episode.append(self.step_penalty)
            reward = 0

        return reward

    def calculate_step_penalty(self, process_length):
        if self.episode_count > self.last_episode_buffered:
            self.episode_lengths_buffer.append(process_length)
        if len(self.episode_lengths_buffer) > self.max_el_buffer_size:
            self.step_penalty = -35. / max(self.episode_lengths_buffer)
            self.episode_lengths_buffer = []

    def reset(self):
        self.send_action(-1)
        self.receive_reward_and_state()

        relative_position = self.position / self.process_length
        self.state = np.array(
            [relative_position, self.reliability, self.predicted_duration])

        return self.state

    def render(self, mode='human'):
        # we don't need this
        return

 #   def close(self):
 #       # print("Closing file and socket...")
 #       self.net.close()
 #       self.socket.close()
 #       print("Closed!")
 #       self.plot_experiment_data()
 #       if self.experiment_number == 0:
 #           self.write_experiment_data_to_csv(os.path.basename(__file__).rstrip(".py"))
 #       else:
 #           self.write_experiment_data_to_csv(
 #               os.path.basename(__file__).rstrip(".py") + "_" + str(self.experiment_number))
 #       ExtendedStateSemiFixedEndNotAdapted.experiment_number += 1

    def save_metrics(self):
        x = np.arange(len(self.cost_list))
        y = np.array(self.cost_list)
        sns.set_style("ticks")
        plt.plot(x, y)
        plt.legend(['action', 'reward', 'cost'], ncol=1, loc='upper left')
        plt.title("step penalty: " + " -(50./process_length)")
        plt.savefig("./results.png")
        plt.show()

        # global df
        # df.to_csv("./metrics.txt", sep=',', index=True, header=False)
