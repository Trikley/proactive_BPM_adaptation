import gym

from stable_baselines.common.policies import MlpPolicy
from stable_baselines.common.vec_env import DummyVecEnv
from stable_baselines import PPO2
import os

inner_env = None


def run(env_string, policy=MlpPolicy, learning_steps=4300, verbose=0, n_steps=128, nminibatches=4, gamma=0.99,
        learning_rate=2.5e-4, ent_coef=0.01, vf_coef=0.5, max_grad_norm=0.5, cliprange=0.2, cliprange_vf=None,
        lam=0.95, policy_kwargs=None, tensorboard_log="tensorboard"):
    global inner_env
    inner_env = gym.make(env_string)
    env = DummyVecEnv([lambda: inner_env])

    model = PPO2(policy=policy, env=env, verbose=verbose, n_steps=n_steps, nminibatches=nminibatches, gamma=gamma,
                 ent_coef=ent_coef, learning_rate=learning_rate, vf_coef=vf_coef, max_grad_norm=max_grad_norm,
                 cliprange=cliprange, cliprange_vf=cliprange_vf, lam=lam, policy_kwargs=policy_kwargs,
                 tensorboard_log=tensorboard_log)
    model.learn(total_timesteps=learning_steps, tb_log_name=os.path.basename(__file__).rstrip(".py"),
                callback=tensorboard_callback)

    env.close()


def tensorboard_callback(locals_, globals_):
    global inner_env
    self_ = locals_['self']
    if inner_env.summary_writer is None:
        inner_env.summary_writer = locals_['writer']

    return True


if __name__ == "__main__":
    run(2000000)
