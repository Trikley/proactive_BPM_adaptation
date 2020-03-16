import os
import pandas as pd
import socket

import gym
import tensorflow as tf
import matplotlib.pyplot as plt
import numpy as np


def get_average_last_entries_from_numeric_list(numeric_list: list, max_entries):
    return sum(numeric_list[-max_entries:]) / max_entries


def get_average_last_entries_from_numeric_list_excluding(numeric_list: list, max_entries, excluded_value, end_index):
    temp_list = numeric_list[np.maximum(0, end_index - max_entries):end_index + 1].copy()
    try:
        while True:
            temp_list.remove(excluded_value)
    except ValueError:
        pass
    if len(temp_list) == 0:
        return 0
    else:
        return sum(temp_list[:]) / len(temp_list)


class BaseEnv(gym.Env):
    show_graphs = False
    log_tensorboard = False
    experiment_number = 1

    def __init__(self):
        self.total_steps = 0
        self.episode_count = 0
        self.adapted_count = 0

        # learning parameter
        self.reward = 0
        self.rewards_per_episode = []
        self.action_value = 0
        self.case_id = -1
        self.actual_duration = -1
        self.predicted_duration = 0
        self.planned_duration = 0
        self.position = 0
        self.cost = 0
        self.process_length = 0
        self.done = 0
        self.adapted = 0
        self.reliability = 0

        self.true = False

        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        # now connect to the web server on port 80 - the normal http port
        self.socket.connect(("threshold-java", 1337))

        self.net = self.socket.makefile("rw", 65536, newline=os.linesep)

        self.summary_writer = None

        # English Motherfucker, do you speak it?
        # Metriken fuer gesamten Verlauf des Experiments, d.h. bei jedem Step:
        self.actions = []  # Action pro Step
        self.rewards = []  # Reward pro Step (-Kosten)
        self.costs = []  # Kosten pro Step (d.h. 50/length bei nicht-adapt)
        # self.violation_predicted = []  # 1 if in each step a violation is predicted
        self.adapted_no_violation = []  # 1 if adapted though no violation was predicted

        # Metriken fuer jede Episode, d.h. jeden Case:
        self.case_id_list = []
        # self.avg_action_per_ep = []  # Durchschnitt ueber alle Aktionen einer Episode
        # self.tmp_avg_action_per_ep = []
        self.avg_adapted_100 = []  # Average end of episode over the last 100, between 1(adapted) and 0(adapted)
        # self.avg_reward_per_ep = []  # Durschnittlicher Reward der Episode
        self.tmp_cumul_reward_per_ep = []
        # self.avg_cost_per_ep = []  # Definitely not the average cost per episode!
        self.tmp_cumul_cost_per_ep = []

        # lists for metrics
        self.cumul_cost_per_ep = []  # Kumulative Kosten pro Episode
        self.cumul_reward_per_ep = []  # Kumulativer Reward pro Episode
        self.true_per_ep = []  # 1 if Episode ending decision is right, 0 otherwise
        self.adapt_in_ep = []  # Info ob in Episode adaptiert wurde
        self.case_length_per_episode = []  # Länge der Episode
        self.position_of_adaptation_per_episode = []  # Position der Adaption pro Episode; -1 := keine Adaption in der Episode
        self.earliness = []  # Position der Adaption durch Länge der Episode, -1 := keine Adaption in der Episode

        self.percentage_true_last_100 = []  # percentage of true decisions among the last 100
        # self.true_positive_per_positive = []  # Only includes episodes that end in adaption, 1 if true, 0 if false
        # self.percentage_true_positive_100 = []  # percentage of true decisions among the last 100 positives
        # self.true_negative_per_negative = []  # Only includes episodes that don't end in adaption, 1 if true, 0 if false
        # self.percentage_true_negative_100 = []  # percentage of true decisions among the last 100 negatives

    def send_action(self, action):
        # print("sending action...")
        self.net.writelines([str(action) + os.linesep])
        self.net.flush()
        # print("action sent: " + str(action))

    def receive_reward_and_state(self):
        # print("receiving reward parameters...")
        adapted = self.net.readline()
        adapted = adapted.strip()
        adapted = True if adapted == 'true' else False
        cost = self.net.readline()
        cost = cost.strip()
        cost = float(cost)
        done = self.net.readline()
        done = done.strip()
        done = True if done == 'true' else False
        if done:
            true = self.net.readline()
            true = true.strip()
            true = True if true == 'true' else False
            self.true = true

        case_id = self.net.readline()
        case_id = case_id.strip()
        case_id = float(case_id)
        actual_duration = self.net.readline()
        actual_duration = actual_duration.strip()
        actual_duration = float(actual_duration)
        predicted_duration = self.net.readline()
        predicted_duration = predicted_duration.strip()
        predicted_duration = float(predicted_duration)
        planned_duration = self.net.readline()
        planned_duration = planned_duration.strip()
        planned_duration = float(planned_duration)
        reliability = self.net.readline()
        reliability = reliability.strip()
        reliability = float(reliability)
        position = self.net.readline()
        position = position.strip()
        position = float(position)
        process_length = self.net.readline()
        process_length = process_length.strip()
        process_length = float(process_length)

        # compute the reward
        reward = self.compute_reward(adapted, cost, done, predicted_duration, planned_duration, reliability, position,
                                     process_length, actual_duration=actual_duration)

        self.adapted = adapted
        self.cost = cost
        self.done = done
        self.case_id = case_id
        self.actual_duration = actual_duration
        self.predicted_duration = predicted_duration
        self.planned_duration = planned_duration
        self.reliability = reliability
        self.reward = reward

        if position != 0.:
            self.position = position
            self.process_length = process_length

        return reward, done, predicted_duration, planned_duration, reliability, position, process_length, cost, adapted

    def log_with_tensorboard(self, tag, simple_value, step):
        if self.summary_writer is not None and BaseEnv.log_tensorboard:
            summary = tf.Summary(
                value=[tf.Summary.Value(tag=tag, simple_value=simple_value)])
            self.summary_writer.add_summary(summary, step)

    def do_logging(self, action):
        # Reward, Kosten und Aktion in Gesamtliste speichern:
        self.rewards.append(self.reward)
        self.costs.append(self.cost)
        self.actions.append(int(action))
        if self.predicted_duration > self.planned_duration:
            # pred_violation = 1
            adap_no_pred = 0
        else:
            # pred_violation = 0
            if int(action) == 1:
                adap_no_pred = 1
            else:
                adap_no_pred = 0

        # self.violation_predicted.append(pred_violation)
        self.adapted_no_violation.append(adap_no_pred)
        # Reward, Kosten und Aktion in temporaeren Listen fuer Episode speichern:
        self.tmp_cumul_cost_per_ep.append(self.cost)
        self.tmp_cumul_reward_per_ep.append(self.reward)
        # self.tmp_avg_action_per_ep.append(int(action))

        # self.log_with_tensorboard(tag='custom/reward', simple_value=self.reward,
        #                           step=self.total_steps)
        # self.log_with_tensorboard(tag='custom/action', simple_value=self.action_value,
        #                           step=self.total_steps)
        # self.log_with_tensorboard(tag='custom/cost', simple_value=-self.cost,
        #                           step=self.total_steps)
        # self.log_with_tensorboard(tag='custom/violation_predicted', simple_value=pred_violation,
        #                           step=self.total_steps)
        self.log_with_tensorboard(tag='custom/adapted_though_no_violation_predicted', simple_value=adap_no_pred,
                                  step=self.total_steps)

        self.total_steps += 1
        if self.total_steps % 1000 == 0:
            if len(self.percentage_true_last_100) > 0:
                print("Step " + str(self.total_steps) + " in episode " + str(self.episode_count) + " with " + str(
                    self.percentage_true_last_100[-1]) + " true decisions")
            else:
                print("Step " + str(self.total_steps) + " in episode " + str(self.episode_count))

        if self.done:  # Ende einer Episode
            self.case_id_list.append(self.case_id)
            # Speichern der kumulativen und durschnittlichen Episodenkosten
            cumul_episode_cost = sum(self.tmp_cumul_cost_per_ep)
            self.cumul_cost_per_ep.append(cumul_episode_cost)
            # avg_episode_cost = cumul_episode_cost / len(self.tmp_cumul_cost_per_ep)
            # self.avg_cost_per_ep.append(avg_episode_cost)
            self.tmp_cumul_cost_per_ep = []

            # Speichern des kumulativen und durschnittlichen Rewards pro Episode
            cumul_episode_reward = sum(self.tmp_cumul_reward_per_ep)

            # avg_episode_reward = cumul_episode_reward / len(self.tmp_cumul_reward_per_ep)
            self.cumul_reward_per_ep.append(cumul_episode_reward)
            # self.avg_reward_per_ep.append(avg_episode_reward)
            self.tmp_cumul_reward_per_ep = []

            # Speichern der durschnittlichen Aktionen einer Episode und ob adaptiert wurde
            # avg_actions = sum(self.tmp_avg_action_per_ep) / len(self.tmp_avg_action_per_ep)
            # self.avg_action_per_ep.append(avg_actions)
            # elf.tmp_avg_action_per_ep = []

            true_negative_status = self.true
            self.true_per_ep.append(true_negative_status)
            if self.adapted:
                self.adapt_in_ep.append(1)

                self.position_of_adaptation_per_episode.append(self.position)
                self.earliness.append(self.position / self.process_length)
                self.log_with_tensorboard(tag='episode_result/earliness',
                                          simple_value=self.earliness[-1],
                                          step=self.episode_count)
                # self.true_positive_per_positive.append(true_negative_status)
            else:
                self.adapt_in_ep.append(0)
                self.position_of_adaptation_per_episode.append(-1)
                self.earliness.append(-1)

            self.case_length_per_episode.append(self.process_length)

            # self.true_negative_per_negative.append(true_negative_status)
            avg_adapted_100_value = sum(self.adapt_in_ep[-100:]) / 100
            self.avg_adapted_100.append(avg_adapted_100_value)

            percentage_true_last_100_value = get_average_last_entries_from_numeric_list(self.true_per_ep, 100)
            self.percentage_true_last_100.append(percentage_true_last_100_value)
            # percentage_true_positive_100_value = get_average_last_entries_from_numeric_list(
            #    self.true_positive_per_positive, 100)
            # self.percentage_true_positive_100.append(percentage_true_positive_100_value)
            # percentage_true_negative_100_value = get_average_last_entries_from_numeric_list(
            #    self.true_negative_per_negative, 100)
            # self.percentage_true_negative_100.append(percentage_true_negative_100_value)

            self.log_with_tensorboard(tag='episode_reward/episode_cost', simple_value=-cumul_episode_cost,
                                      step=self.episode_count)
            # self.log_with_tensorboard(tag='episode_reward/episode_reward*', simple_value=cumul_episode_reward,
            #                           step=self.episode_count)
            # self.log_with_tensorboard(tag='episode_result/adapted', simple_value=self.adapted,
            #                           step=self.episode_count)
            self.log_with_tensorboard(tag='episode_result/average_adapted_last_100_episodes',
                                      simple_value=avg_adapted_100_value,
                                      step=self.episode_count)
            # self.log_with_tensorboard(tag='episode_result/average_action', simple_value=avg_actions,
            #                           step=self.episode_count)
            # self.log_with_tensorboard(tag='episode_result/ended_correctly', simple_value=true_negative_status,
            #                           step=self.episode_count)
            # if self.adapted:
            #     self.log_with_tensorboard(tag='episode_result/positives_ended_correctly',
            #                               simple_value=true_negative_status,
            #                               step=self.adapted_count)
            # else:
            #     self.log_with_tensorboard(tag='episode_result/negatives_ended_correctly',
            #                               simple_value=true_negative_status,
            #                               step=self.episode_count - self.adapted_count)
            self.log_with_tensorboard(tag='episode_result/percentage_of_trues_among_last_100_episodes',
                                      simple_value=percentage_true_last_100_value,
                                      step=self.episode_count)
            # self.log_with_tensorboard(tag='episode_result/percentage_of_trues_among_last_100_positives',
            #                           simple_value=percentage_true_positive_100_value,
            #                           step=self.episode_count)
            # self.log_with_tensorboard(tag='episode_result/percentage_of_trues_among_last_100_negatives',
            #                           simple_value=percentage_true_negative_100_value,
            #                           step=self.episode_count)
            #
            # self.log_with_tensorboard(tag='episode_result/percentage_Correct_Adaptation_Decisions',
            #                           simple_value=(self.true_per_ep[-1000:].count(1) / 1000) * 100,
            #                           step=self.episode_count)
            # self.log_with_tensorboard(tag='episode_result/adapt_in_ep',
            #                           simple_value=(self.adapt_in_ep.count(1) / self.adapt_in_ep.__len__()) * 100,
            #                           step=self.episode_count)
            self.episode_count += 1
            if self.adapted:
                self.adapted_count += 1

    def compute_reward(self, adapted, cost, done, predicted_duration, planned_duration, reliability, position,
                       process_length, actual_duration=0.):
        pass

    def reset(self):
        pass

    def render(self, mode='human'):
        pass

    def step(self, action):
        pass

    def close(self):
        # print("Closing file and socket...")
        self.net.close()
        self.socket.close()
        print("Closed!")
        self.plot_experiment_data()
        self.write_experiment_data_to_csv(
            os.path.basename(self.__class__.__name__) + "_" + str(BaseEnv.experiment_number))
        BaseEnv.experiment_number += 1

    def plot_experiment_data(self):
        print("plotting_data")

    def write_experiment_data_to_csv(self, csv_name):

        earliness_avg = []
        true_avg_100 = []
        true_avg_1000 = []
        adapt_avg = []
        cost_avg = []
        reward_avg = []
        for ep in range(0, len(self.true_per_ep)):
            earliness_avg.append(get_average_last_entries_from_numeric_list_excluding(self.earliness, 100, -1, ep))
            true_avg_100.append(get_average_last_entries_from_numeric_list_excluding(self.true_per_ep, 100, -1, ep))
            true_avg_1000.append(get_average_last_entries_from_numeric_list_excluding(self.true_per_ep, 1000, -1, ep))
            adapt_avg.append(get_average_last_entries_from_numeric_list_excluding(self.adapt_in_ep, 100, -1, ep))
            cost_avg.append(get_average_last_entries_from_numeric_list_excluding(self.cumul_cost_per_ep, 100, -1, ep))
            reward_avg.append(
                get_average_last_entries_from_numeric_list_excluding(self.cumul_reward_per_ep, 100, -1, ep))

        dataframe_of_metrics = pd.DataFrame(list(zip(self.case_id_list,
                                                     earliness_avg,
                                                     true_avg_100,
                                                     true_avg_1000,
                                                     adapt_avg,
                                                     self.adapt_in_ep,
                                                     cost_avg,
                                                     self.cumul_cost_per_ep,
                                                     reward_avg,
                                                     self.cumul_reward_per_ep,
                                                     self.true_per_ep,
                                                     self.position_of_adaptation_per_episode,
                                                     self.case_length_per_episode,
                                                     )),
                                            columns=['case_id',
                                                     'earliness_avg',
                                                     'true_avg_100',
                                                     'true_avg_1000',
                                                     'adaption_rate_avg',
                                                     'adapt_per_ep',
                                                     'costs_avg',
                                                     'cost_per_ep',
                                                     'rewards_avg',
                                                     'reward_per_ep',
                                                     'true_per_ep',
                                                     'position_adaptation_per_ep',
                                                     'case_length_per_ep'
                                                     ])

        dataframe_of_metrics.plot(subplots=True)

        plt.tight_layout()
        if BaseEnv.show_graphs:
            plt.show()

        dataframe_of_metrics.to_csv(csv_name + '_metrics_of_episodes.csv', header=True, index=False)

        #
        # print("not writing_data")
        # # Gesamtdaten schreiben
        # with open(csv_name + "_results_total.csv", "w", newline="") as f:
        #     for _ in range(0, len(self.actions)):
        #         f.write(str(
        #             str(self.actions[_]) + "," + str(self.rewards[_]) + "," + str(self.costs[_]) + "\n"))
        #     print("Percentage Correct Adaptation Decisions among the last 1000 episodes: " + str(
        #         (sum(self.true_per_ep[-1000:]) / 1000) * 100))
        # print("Percentage Adapted among the last 1000 episodes: " + str((sum(self.adapt_in_ep[-1000:]) / 1000) * 100))
        #
        # # Episodendaten
        #
        # with open(csv_name + "_results_per_episode.csv", "w", newline="") as f:
        #     f.write(
        #         "case_id,adapted,cumul_cost,true,percentage_true_last_100,cumul_reward,earliness," +
        #         "earliness_avg_last_100\n")
        #     for _ in range(0, len(self.true_per_ep)):
        #         f.write(str(
        #             str(self.case_id_list[_]) + "," + str(self.adapt_in_ep[_]) + "," + str(
        #                 self.cumul_cost_per_ep[_]) + "," + str(
        #                 self.true_per_ep[_]) + "," + str(self.percentage_true_last_100[_]) + "," + str(
        #                 self.cumul_reward_per_ep[_]) + "," + str(self.earliness[_]) + "," + str(
        #                 get_average_last_entries_from_numeric_list_excluding(self.earliness, 100, -1, _)) + "\n"))
