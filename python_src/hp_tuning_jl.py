import gym
import numpy as np
import tensorflow as tf
from stable_baselines.common.noise import OrnsteinUhlenbeckActionNoise

from stable_baselines.common.policies import MlpPolicy
from stable_baselines.common.vec_env import DummyVecEnv
from stable_baselines import PPO2

environment = None


def learn(algorithm, environment_name, total_timesteps=1000, n_steps=128, gamma=0.99, nminibatches=4, verbose=0):
    global environment
    environment = gym.make('gym_threshold:' + environment_name)
    dummy_vec_environment = DummyVecEnv([lambda: environment])

    if algorithm == "PPO2":
        model = PPO2(MlpPolicy, dummy_vec_environment,
                     verbose=verbose,
                     n_steps=n_steps,
                     gamma=gamma,
                     nminibatches=nminibatches,
                     cliprange_vf=-1,
                     tensorboard_log="tensorboard"
                     )
    else:
        raise AttributeError('No algorithm with name: {}'.format(algorithm))

    model.learn(total_timesteps=total_timesteps,
                tb_log_name="algorithm: {}, n_steps: {}, nminibatches: {}, gamma: {} run".format(algorithm, n_steps,
                                                                                                 nminibatches, gamma),
                callback=tensorboard_callback)

    dummy_vec_environment.close()


def tensorboard_callback(locals_, globals_):
    global environment
    self_ = locals_['self']

    summary = tf.Summary(value=[tf.Summary.Value(tag='reward', simple_value=environment.reward)])
    locals_['writer'].add_summary(summary, self_.num_timesteps)

    summary = tf.Summary(value=[tf.Summary.Value(tag='action', simple_value=environment.action_value)])
    locals_['writer'].add_summary(summary, self_.num_timesteps)
    return True


for n_steps in [2, 2, 8, 16, 32, 64]:
    learn(algorithm="PPO2",
          environment_name="threshold-without-state-v0",
          total_timesteps=20000,
          n_steps=n_steps,
          gamma=0,
          nminibatches=1,
          verbose=0)
