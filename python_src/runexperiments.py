from python_src.experiments import ppo2_extended_space_semi_fixed_end_reward_not_adapted, \
    ppo2_semi_fixed_end_reward_not_adapted_maintain_ratio, ppo2lstm_extended_space_semi_fixed_end_reward_not_adapted, \
    ppo2lstm_semi_fixed_end_reward_not_adapted_maintain_ratio, ppo2_parameter_environment
from gym_threshold.envs.cnn_state_cost_reward import custom_cnn
from gym_threshold.envs.baseenv import BaseEnv
from stable_baselines.common.policies import MlpPolicy, CnnPolicy

import time

BaseEnv.show_graphs = False
steps = 250000

for _ in range(4):
    ppo2_parameter_environment.run(env_string="gym_threshold:master-state-master-reward-v0", learning_steps=steps,
                                   gamma=1, tensorboard_log=None)
    time.sleep(10)

