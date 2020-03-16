import os

import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
from gym import spaces
from gym_threshold.envs.baseenv import BaseEnv, get_average_last_entries_from_numeric_list


class RawDataEnv(BaseEnv):
    metadata = {'render.modes': ['human']}
    summary_writer = None

    def __init__(self):
        super().__init__()
        #################################
        # Parameter fuer das Environment
        #################################

        self.reliability_steps = []
        self.predicted_duration_steps = []
        self.violation_predicted_steps = []

        self.tmp_reliability = 0
        self.tmp_predicted_duration = 0
        self.step_counter = 0

    def step(self, action=None):
        self.send_action(int(0))

        self.receive_reward_and_state()

        if self.done:
            v = self.true
            self.reliability_steps.append(self.tmp_reliability)
            self.predicted_duration_steps.append(self.tmp_predicted_duration)

            while self.violation_predicted_steps.__len__() < len(self.reliability_steps):
                self.violation_predicted_steps.append(v)

        else:
            self.tmp_reliability = self.reliability
            self.tmp_predicted_duration = self.predicted_duration

            self.step_counter = -1

        self.step_counter += 1

        return self.done

    def compute_reward(self, adapted, cost, done, predicted_duration, planned_duration, reliability, position,
                       process_length):
        return 0

    def reset(self):
        self.send_action(-1)
        self.receive_reward_and_state()

        self.state = np.array(
            [self.position, self.process_length, self.reliability, self.planned_duration, self.predicted_duration])

        return self.state

    def render(self, mode='human'):
        # we don't need this
        return

    def close(self):
        # print("Closing file and socket...")
        self.net.close()
        self.socket.close()
        print("Closed!")
        self.plot_experiment_data()
        self.write_experiment_data_to_csv(os.path.basename(__file__).rstrip(".py"))

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


def correlation_coefficient(x, y):
    mean_x = np.mean(x)
    mean_y = np.mean(y)

    x_mean_sum = np.sum(np.square(np.add(x, -mean_x)))
    y_mean_sum = np.sum(np.square(np.add(y, -mean_y)))

    n = np.sqrt(x_mean_sum * y_mean_sum)

    z = np.sum(np.multiply(np.add(x, -mean_x),
                           np.add(y, -mean_y)))

    return z / n


def pearson_correlation_coefficient(x, y, z):
    rrp = correlation_coefficient(x, y)

    c = np.array([correlation_coefficient(x, z),
                  correlation_coefficient(y, z)])

    R = np.linalg.inv(np.array([[1., rrp], [rrp, 1.]]))

    return np.dot(np.dot(c, R), c)


if __name__ == '__main__':

    env = RawDataEnv()
    episode_counter = 0

    while episode_counter < 30000:

        if env.step():
            episode_counter += 1

            if episode_counter % 1000 == 0:
                print(episode_counter)

    x = np.array(env.reliability_steps)
    y = np.array(env.predicted_duration_steps)

    z = np.array(env.violation_predicted_steps)

    print(pearson_correlation_coefficient(x, y, z))
