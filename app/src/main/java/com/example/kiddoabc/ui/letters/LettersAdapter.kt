package com.example.kiddoabc.ui.letters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kiddoabc.data.models.Letter
import com.example.kiddoabc.databinding.ItemLetterBinding

class LettersAdapter(
    private val onLetterClick: (Letter) -> Unit,
    private val onLetterLongClick: (Letter) -> Unit
) : ListAdapter<Letter, LettersAdapter.LetterViewHolder>(LetterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LetterViewHolder {
        val binding = ItemLetterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LetterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LetterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LetterViewHolder(
        private val binding: ItemLetterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(letter: Letter) {
            binding.tvLetter.text = letter.character

            // Click simple : jouer le son
            binding.root.setOnClickListener {
                onLetterClick(letter)
            }

            // Long click : ouvrir l'écran de traçage
            binding.root.setOnLongClickListener {
                onLetterLongClick(letter)
                true
            }
        }
    }

    private class LetterDiffCallback : DiffUtil.ItemCallback<Letter>() {
        override fun areItemsTheSame(oldItem: Letter, newItem: Letter): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Letter, newItem: Letter): Boolean {
            return oldItem == newItem
        }
    }
}